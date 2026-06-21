package com.example.sketchto3view.ui.history

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sketchto3view.data.service.GenerationServiceManager
import com.example.sketchto3view.domain.model.DownloadResult
import com.example.sketchto3view.domain.model.GenerationTask
import com.example.sketchto3view.domain.model.TaskStatus
import com.example.sketchto3view.domain.repository.TaskRepository
import com.example.sketchto3view.domain.service.DownloadManagerService
import com.example.sketchto3view.domain.usecase.DownloadImagesUseCase
import com.example.sketchto3view.domain.usecase.SubmitImagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val downloadImagesUseCase: DownloadImagesUseCase,
    private val submitImagesUseCase: SubmitImagesUseCase,
    private val generationServiceManager: GenerationServiceManager,
    private val downloadManagerService: DownloadManagerService
) : ViewModel() {

    data class UiState(
        val tasks: List<GenerationTask> = emptyList(),
        val isGridView: Boolean = true,
        val isDownloadingAll: Boolean = false,
        val downloadMessage: String? = null,
        val activeTaskCount: Int = 0,
        val queuedTaskCount: Int = 0,
        val showShareButton: Boolean = false,
        val selectedTaskIds: Set<String> = emptySet(),
        val isSelectionMode: Boolean = false
    )

    private val _isGridView = MutableStateFlow(true)
    private val _isDownloadingAll = MutableStateFlow(false)
    private val _downloadMessage = MutableStateFlow<String?>(null)
    private val _showShareButton = MutableStateFlow(false)
    private val _selectedTaskIds = MutableStateFlow<Set<String>>(emptySet())
    private var _lastDownloadUri: android.net.Uri? = null

    val uiState: StateFlow<UiState> = combine(
        taskRepository.getAllTasks(),
        _isGridView,
        _isDownloadingAll,
        _downloadMessage,
        generationServiceManager.serviceStatus,
        _showShareButton,
        _selectedTaskIds
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val tasks = values[0] as List<GenerationTask>
        val isGridView = values[1] as Boolean
        val isDownloadingAll = values[2] as Boolean
        val downloadMessage = values[3] as String?
        val serviceStatus = values[4] as com.example.sketchto3view.data.service.GenerationServiceManager.ServiceStatus
        val showShareButton = values[5] as Boolean
        @Suppress("UNCHECKED_CAST")
        val selectedIds = values[6] as Set<String>
        UiState(
            tasks = tasks.sortedByDescending { it.createdAt },
            isGridView = isGridView,
            isDownloadingAll = isDownloadingAll,
            downloadMessage = downloadMessage,
            activeTaskCount = serviceStatus.activeCount,
            queuedTaskCount = serviceStatus.queuedCount,
            showShareButton = showShareButton,
            selectedTaskIds = selectedIds,
            isSelectionMode = selectedIds.isNotEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState()
    )

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun retryTask(taskId: String) {
        viewModelScope.launch {
            submitImagesUseCase.retry(taskId)
        }
    }

    fun downloadAllTasks() {
        viewModelScope.launch {
            _isDownloadingAll.value = true
            _downloadMessage.value = null
            _showShareButton.value = false
            try {
                val result = downloadImagesUseCase.downloadAllTasks()
                val completedCount = uiState.value.tasks.count { it.status == TaskStatus.COMPLETED }
                when (result) {
                    is DownloadResult.Success -> {
                        _downloadMessage.value = "批量下载完成! ${completedCount}个任务已打包"
                        _lastDownloadUri = result.uri
                        _showShareButton.value = true
                    }
                    is DownloadResult.Failure -> {
                        _downloadMessage.value = "下载失败: ${result.message}"
                    }
                }
            } catch (e: Exception) {
                _downloadMessage.value = "下载失败: ${e.message}"
            } finally {
                _isDownloadingAll.value = false
            }
        }
    }

    fun shareLastDownload() {
        _lastDownloadUri?.let { uri ->
            downloadManagerService.shareFile(uri, "application/zip")
        }
    }

    fun clearDownloadMessage() {
        _downloadMessage.value = null
    }

    fun toggleTaskSelection(taskId: String) {
        val current = _selectedTaskIds.value
        _selectedTaskIds.value = if (taskId in current) current - taskId else current + taskId
    }

    fun clearSelection() {
        _selectedTaskIds.value = emptySet()
    }

    fun downloadSelected() {
        viewModelScope.launch {
            val selectedIds = _selectedTaskIds.value
            if (selectedIds.isEmpty()) return@launch

            _isDownloadingAll.value = true
            _downloadMessage.value = null
            _showShareButton.value = false
            try {
                val result = downloadImagesUseCase.downloadSelectedTasks(selectedIds)
                when (result) {
                    is DownloadResult.Success -> {
                        _downloadMessage.value = "批量下载完成! ${selectedIds.size}个任务已打包"
                        _lastDownloadUri = result.uri
                        _showShareButton.value = true
                        _selectedTaskIds.value = emptySet()
                    }
                    is DownloadResult.Failure -> {
                        _downloadMessage.value = "下载失败: ${result.message}"
                    }
                }
            } catch (e: Exception) {
                _downloadMessage.value = "下载失败: ${e.message}"
            } finally {
                _isDownloadingAll.value = false
            }
        }
    }
}
