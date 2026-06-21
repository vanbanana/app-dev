package com.example.sketchto3view.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sketchto3view.data.service.GenerationServiceManager
import com.example.sketchto3view.domain.model.DownloadResult
import com.example.sketchto3view.domain.model.GenerationTask
import com.example.sketchto3view.domain.model.TaskStatus
import com.example.sketchto3view.domain.repository.TaskRepository
import com.example.sketchto3view.domain.usecase.DownloadImagesUseCase
import com.example.sketchto3view.domain.usecase.SubmitImagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val submitImagesUseCase: SubmitImagesUseCase,
    private val downloadImagesUseCase: DownloadImagesUseCase,
    private val generationServiceManager: GenerationServiceManager
) : ViewModel() {

    data class UiState(
        val tasks: List<GenerationTask> = emptyList(),
        val isLoading: Boolean = false,
        val currentProcessingTask: GenerationTask? = null,
        val completedTaskCount: Int = 0,
        val downloadAllMessage: String? = null,
        val isDownloadingAll: Boolean = false,
        val activeTaskCount: Int = 0,
        val queuedTaskCount: Int = 0,
        val totalTaskCount: Int = 0
    )

    private val _isLoading = MutableStateFlow(false)
    private val _downloadAllMessage = MutableStateFlow<String?>(null)
    private val _isDownloadingAll = MutableStateFlow(false)

    private val _completionEvent = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val completionEvent: SharedFlow<String> = _completionEvent.asSharedFlow()

    // Track previously known completed task IDs to detect new completions
    private var previousCompletedTaskIds = setOf<String>()

    val uiState: StateFlow<UiState> = combine(
        taskRepository.getAllTasks(),
        _isLoading,
        _downloadAllMessage,
        _isDownloadingAll,
        generationServiceManager.serviceStatus
    ) { tasks, isLoading, downloadMessage, isDownloadingAll, serviceStatus ->
        UiState(
            tasks = tasks,
            isLoading = isLoading,
            currentProcessingTask = tasks.firstOrNull {
                it.status == TaskStatus.PROCESSING || it.status == TaskStatus.QUEUED
            },
            completedTaskCount = tasks.count { it.status == TaskStatus.COMPLETED },
            downloadAllMessage = downloadMessage,
            isDownloadingAll = isDownloadingAll,
            activeTaskCount = serviceStatus.activeCount,
            queuedTaskCount = serviceStatus.queuedCount,
            totalTaskCount = serviceStatus.totalCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState()
    )

    init {
        // Observe task list to detect newly completed tasks
        viewModelScope.launch {
            var isFirstEmission = true
            taskRepository.getAllTasks().collect { tasks ->
                val currentCompletedIds = tasks
                    .filter { it.status == TaskStatus.COMPLETED }
                    .map { it.id }
                    .toSet()

                if (isFirstEmission) {
                    // On first emission, just record current state without triggering events
                    previousCompletedTaskIds = currentCompletedIds
                    isFirstEmission = false
                } else {
                    // Only emit for truly new completions
                    val newlyCompleted = currentCompletedIds - previousCompletedTaskIds
                    newlyCompleted.forEach { taskId ->
                        _completionEvent.tryEmit(taskId)
                    }
                    previousCompletedTaskIds = currentCompletedIds
                }
            }
        }
    }

    fun submitImages(
        imageUris: List<Uri>,
        style: com.example.sketchto3view.data.api.DefaultImageGenerationApi.Companion.PromptStyle = com.example.sketchto3view.data.api.DefaultImageGenerationApi.Companion.PromptStyle.REALISTIC
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                submitImagesUseCase(imageUris, style)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun retryTask(taskId: String) {
        viewModelScope.launch {
            submitImagesUseCase.retry(taskId)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }

    fun downloadAllTasks() {
        viewModelScope.launch {
            _isDownloadingAll.value = true
            _downloadAllMessage.value = null
            try {
                val result = downloadImagesUseCase.downloadAllTasks()
                when (result) {
                    is DownloadResult.Success -> {
                        _downloadAllMessage.value = "下载成功! 已保存到 Downloads"
                    }
                    is DownloadResult.Failure -> {
                        _downloadAllMessage.value = "下载失败: ${result.message}"
                    }
                }
            } catch (e: Exception) {
                _downloadAllMessage.value = "下载失败: ${e.message}"
            } finally {
                _isDownloadingAll.value = false
            }
        }
    }

    fun clearDownloadMessage() {
        _downloadAllMessage.value = null
    }
}
