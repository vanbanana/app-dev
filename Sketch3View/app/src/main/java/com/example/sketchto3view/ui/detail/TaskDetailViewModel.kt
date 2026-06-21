package com.example.sketchto3view.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sketchto3view.domain.model.DownloadResult
import com.example.sketchto3view.domain.model.GenerationTask
import com.example.sketchto3view.domain.repository.TaskRepository
import com.example.sketchto3view.domain.service.DownloadManagerService
import com.example.sketchto3view.domain.usecase.DownloadImagesUseCase
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
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val downloadImagesUseCase: DownloadImagesUseCase,
    private val downloadManagerService: DownloadManagerService
) : ViewModel() {

    private val taskId: String = savedStateHandle.get<String>("taskId") ?: ""

    data class UiState(
        val task: GenerationTask? = null,
        val isDownloading: Boolean = false
    )

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    init {
        loadTask()
    }

    private fun loadTask() {
        viewModelScope.launch {
            val task = taskRepository.getTask(taskId)
            _uiState.value = _uiState.value.copy(task = task)
        }
    }

    fun refreshTask() {
        loadTask()
    }

    fun downloadAsZip() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true)
            val result = downloadImagesUseCase.downloadAsZip(taskId)
            _uiState.value = _uiState.value.copy(isDownloading = false)
            when (result) {
                is DownloadResult.Success -> {
                    _events.emit(UiEvent.ShowSnackbar("下载成功! 已保存到 Downloads"))
                }
                is DownloadResult.Failure -> {
                    _events.emit(UiEvent.ShowSnackbar("下载失败: ${result.message}"))
                }
            }
        }
    }

    fun downloadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true)
            val results = downloadImagesUseCase.downloadAll(taskId)
            _uiState.value = _uiState.value.copy(isDownloading = false)
            val failures = results.filterIsInstance<DownloadResult.Failure>()
            if (failures.isEmpty()) {
                _events.emit(UiEvent.ShowSnackbar("下载成功! 已保存到 Downloads"))
            } else {
                _events.emit(UiEvent.ShowSnackbar("下载失败: ${failures.first().message}"))
            }
        }
    }

    fun downloadSingle(viewIndex: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true)
            val result = downloadImagesUseCase.downloadSingle(taskId, viewIndex)
            _uiState.value = _uiState.value.copy(isDownloading = false)
            when (result) {
                is DownloadResult.Success -> {
                    _events.emit(UiEvent.ShowSnackbar("下载成功! 已保存到 Downloads"))
                }
                is DownloadResult.Failure -> {
                    _events.emit(UiEvent.ShowSnackbar("下载失败: ${result.message}"))
                }
            }
        }
    }

    fun shareAsZip() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true)
            val result = downloadImagesUseCase.downloadAsZip(taskId)
            _uiState.value = _uiState.value.copy(isDownloading = false)
            when (result) {
                is DownloadResult.Success -> {
                    downloadManagerService.shareFile(result.uri, "application/zip")
                }
                is DownloadResult.Failure -> {
                    _events.emit(UiEvent.ShowSnackbar("分享失败: ${result.message}"))
                }
            }
        }
    }

    fun shareImage(viewIndex: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true)
            val result = downloadImagesUseCase.downloadSingle(taskId, viewIndex)
            _uiState.value = _uiState.value.copy(isDownloading = false)
            when (result) {
                is DownloadResult.Success -> {
                    downloadManagerService.shareFile(result.uri, "image/png")
                }
                is DownloadResult.Failure -> {
                    _events.emit(UiEvent.ShowSnackbar("分享失败: ${result.message}"))
                }
            }
        }
    }
}
