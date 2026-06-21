package com.example.sketchto3view.ui.crop

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sketchto3view.domain.model.CropLine
import com.example.sketchto3view.domain.model.CropResult
import com.example.sketchto3view.domain.model.GenerationTask
import com.example.sketchto3view.domain.model.Orientation
import com.example.sketchto3view.domain.repository.TaskRepository
import com.example.sketchto3view.domain.usecase.CropImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManualCropViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val cropImageUseCase: CropImageUseCase
) : ViewModel() {

    private val taskId: String = savedStateHandle.get<String>("taskId") ?: ""

    data class CropState(
        val task: GenerationTask? = null,
        val threeViewImagePath: String? = null,
        val cropLines: List<Float> = listOf(0.33f, 0.66f), // Two vertical crop lines
        val isProcessing: Boolean = false
    )

    sealed class UiEvent {
        object CropSuccess : UiEvent()
        data class CropError(val message: String) : UiEvent()
    }

    private val _cropState = MutableStateFlow(CropState())
    val cropState: StateFlow<CropState> = _cropState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    init {
        loadTask()
    }

    private fun loadTask() {
        viewModelScope.launch {
            val task = taskRepository.getTask(taskId)
            _cropState.value = _cropState.value.copy(
                task = task,
                threeViewImagePath = task?.threeViewImagePath
            )
        }
    }

    fun updateCropLine(index: Int, position: Float) {
        val currentLines = _cropState.value.cropLines.toMutableList()
        if (index in currentLines.indices) {
            // Clamp position between 0.05 and 0.95, and ensure ordering
            val clampedPosition = position.coerceIn(0.05f, 0.95f)
            currentLines[index] = clampedPosition

            // Ensure lines don't cross each other
            if (index == 0 && currentLines.size > 1) {
                currentLines[0] = clampedPosition.coerceAtMost(currentLines[1] - 0.05f)
            } else if (index == 1 && currentLines.isNotEmpty()) {
                currentLines[1] = clampedPosition.coerceAtLeast(currentLines[0] + 0.05f)
            }

            _cropState.value = _cropState.value.copy(cropLines = currentLines)
        }
    }

    fun confirmCrop() {
        viewModelScope.launch {
            _cropState.value = _cropState.value.copy(isProcessing = true)

            val cropLines = _cropState.value.cropLines.map { position ->
                CropLine(
                    orientation = Orientation.VERTICAL,
                    position = position
                )
            }

            val result = cropImageUseCase.manualCrop(taskId, cropLines)

            _cropState.value = _cropState.value.copy(isProcessing = false)

            when (result) {
                is CropResult.Success -> {
                    _events.emit(UiEvent.CropSuccess)
                }
                is CropResult.DetectionFailed -> {
                    _events.emit(UiEvent.CropError(result.reason))
                }
            }
        }
    }
}
