package com.example.sketchto3view.domain.usecase

import android.graphics.BitmapFactory
import android.net.Uri
import com.example.sketchto3view.data.api.DefaultImageGenerationApi
import com.example.sketchto3view.data.local.TaskDao
import com.example.sketchto3view.data.queue.TaskQueue
import com.example.sketchto3view.data.service.GenerationServiceManager
import com.example.sketchto3view.data.service.NotificationHelper
import com.example.sketchto3view.domain.model.GenerationRequest
import com.example.sketchto3view.domain.model.GenerationResult
import com.example.sketchto3view.domain.model.GenerationTask
import com.example.sketchto3view.domain.model.TaskStatus
import com.example.sketchto3view.domain.repository.TaskRepository
import com.example.sketchto3view.domain.service.CropperService
import com.example.sketchto3view.domain.service.ImageGenerationApi
import java.io.File
import javax.inject.Inject

/**
 * Use case for submitting images for three-view generation.
 * Creates a generation task for each submitted image and enqueues them for processing.
 *
 * For each URI:
 * 1. Creates a task via the repository (copies source image, persists metadata)
 * 2. Enqueues the task to the TaskQueue with an executor that:
 *    - Reads the source image bytes
 *    - Calls the API with THREE_VIEW_PROMPT
 *    - On success: saves the three-view image and triggers auto-crop
 *    - On failure: updates task status to FAILED
 *
 * Background resilience:
 * - When API returns a URL, the URL is saved to DB immediately
 * - If download fails (e.g., app goes to background), the URL persists
 * - On next app startup, pending downloads are resumed
 */
class SubmitImagesUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val taskQueue: TaskQueue,
    private val imageGenerationApi: ImageGenerationApi,
    private val cropperService: CropperService,
    private val generationServiceManager: GenerationServiceManager,
    private val notificationHelper: NotificationHelper,
    private val taskDao: TaskDao
) {
    /**
     * Retries a failed task by re-enqueuing the EXISTING task (no new task created).
     * This avoids the duplicate task problem that occurs when calling invoke() for retry.
     *
     * @param taskId The ID of the failed task to retry
     * @param style The prompt style to use (REALISTIC or CHIBI)
     */
    suspend fun retry(
        taskId: String,
        style: DefaultImageGenerationApi.Companion.PromptStyle = DefaultImageGenerationApi.Companion.PromptStyle.REALISTIC
    ) {
        val task = taskRepository.getTask(taskId) ?: return
        if (task.status != TaskStatus.FAILED && task.status != TaskStatus.QUEUED) return

        // Reset status to QUEUED
        taskRepository.updateTaskStatus(taskId, TaskStatus.QUEUED)

        // Re-enqueue the existing task with the same executor logic (no new task created)
        taskQueue.enqueue(task.copy(status = TaskStatus.QUEUED, errorMessage = null)) { generationTask ->
            executeTask(generationTask, style)
        }

        // Notify service manager
        generationServiceManager.onTasksSubmitted(1)
    }

    /**
     * Submits a list of image URIs for three-view generation.
     *
     * @param imageUris List of content URIs pointing to source images
     * @param style The prompt style to use (REALISTIC or CHIBI)
     * @return List of created task IDs in the same order as the input URIs
     */
    suspend operator fun invoke(
        imageUris: List<Uri>,
        style: DefaultImageGenerationApi.Companion.PromptStyle = DefaultImageGenerationApi.Companion.PromptStyle.REALISTIC
    ): List<String> {
        val taskIds = imageUris.map { uri ->
            val task = taskRepository.createTask(uri)

            taskQueue.enqueue(task) { generationTask ->
                executeTask(generationTask, style)
            }

            task.id
        }

        // Notify service manager to start foreground service
        generationServiceManager.onTasksSubmitted(taskIds.size)

        return taskIds
    }

    /**
     * Resumes pending downloads for tasks that have a saved URL but were interrupted.
     * Called on app startup after resetting stuck tasks.
     */
    suspend fun resumePendingDownloads() {
        val pendingTasks = taskDao.getTasksWithPendingDownload()
        for (task in pendingTasks) {
            val url = task.pendingImageUrl ?: continue
            try {
                val imageBytes = imageGenerationApi.downloadImageFromUrl(url)
                if (imageBytes != null && imageBytes.isNotEmpty()) {
                    // Download succeeded - save image, crop, mark completed
                    taskRepository.saveThreeViewImage(task.id, imageBytes)
                    taskDao.clearPendingImageUrl(task.id)

                    // Trigger auto-crop
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    if (bitmap != null) {
                        val cropResult = cropperService.autoCrop(bitmap)
                        when (cropResult) {
                            is com.example.sketchto3view.domain.model.CropResult.Success -> {
                                taskRepository.saveCroppedImages(task.id, cropResult.images)
                                taskRepository.updateTaskStatus(task.id, TaskStatus.COMPLETED)
                                notificationHelper.showCompletionNotification(task.id)
                            }
                            is com.example.sketchto3view.domain.model.CropResult.DetectionFailed -> {
                                taskRepository.updateTaskStatus(task.id, TaskStatus.COMPLETED)
                                notificationHelper.showCompletionNotification(task.id)
                            }
                        }
                    } else {
                        taskRepository.updateTaskStatus(
                            task.id,
                            TaskStatus.FAILED,
                            "下载成功但无法解码图片"
                        )
                        taskDao.clearPendingImageUrl(task.id)
                    }
                } else {
                    // Download still failed - mark as FAILED
                    taskRepository.updateTaskStatus(
                        task.id,
                        TaskStatus.FAILED,
                        "图片下载失败，URL可能已过期，请重试"
                    )
                    taskDao.clearPendingImageUrl(task.id)
                }
            } catch (e: Exception) {
                taskRepository.updateTaskStatus(
                    task.id,
                    TaskStatus.FAILED,
                    "恢复下载失败: ${e.message}"
                )
                taskDao.clearPendingImageUrl(task.id)
            }
        }
    }

    /**
     * Shared task execution logic used by both invoke() and retry().
     * Handles the full lifecycle: PROCESSING -> API call -> save result -> COMPLETED/FAILED.
     *
     * Background resilience flow for URL responses:
     * 1. API returns URL -> save URL to DB immediately
     * 2. Try to download the image
     * 3. If download succeeds: save image, clear pendingImageUrl, continue with crop
     * 4. If download fails: DON'T mark as FAILED. Leave status as PROCESSING with URL saved.
     *    The URL will be retried on next app startup via resumePendingDownloads().
     */
    private suspend fun executeTask(
        generationTask: GenerationTask,
        style: DefaultImageGenerationApi.Companion.PromptStyle
    ) {
        try {
            // Update status to PROCESSING
            taskRepository.updateTaskStatus(generationTask.id, TaskStatus.PROCESSING)

            // Read source image bytes from the stored file
            val sourceFile = File(generationTask.sourceImagePath)
            val sourceImageBytes = sourceFile.readBytes()

            // Call API with style-specific prompt
            val request = GenerationRequest(
                sourceImage = sourceImageBytes,
                prompt = DefaultImageGenerationApi.buildPrompt(style)
            )
            val result = imageGenerationApi.generateThreeView(request)

            when (result) {
                is GenerationResult.Success -> {
                    // b64_json response - image bytes already available
                    handleSuccessfulImageData(generationTask.id, result.imageData)
                }
                is GenerationResult.SuccessUrl -> {
                    // URL response - save URL first for background resilience
                    taskDao.updatePendingImageUrl(generationTask.id, result.imageUrl)

                    // Try to download the image
                    val imageBytes = imageGenerationApi.downloadImageFromUrl(result.imageUrl)
                    if (imageBytes != null && imageBytes.isNotEmpty()) {
                        // Download succeeded
                        taskDao.clearPendingImageUrl(generationTask.id)
                        handleSuccessfulImageData(generationTask.id, imageBytes)
                    } else {
                        // Download failed - DON'T mark as FAILED.
                        // Leave status as PROCESSING with the URL saved.
                        // It will be retried on next app startup via resumePendingDownloads().
                    }
                }
                is GenerationResult.Error -> {
                    taskRepository.updateTaskStatus(
                        generationTask.id,
                        TaskStatus.FAILED,
                        "API error (${result.code}): ${result.message}"
                    )
                }
            }
        } catch (e: Exception) {
            taskRepository.updateTaskStatus(
                generationTask.id,
                TaskStatus.FAILED,
                "Unexpected error: ${e.message}"
            )
        }
    }

    /**
     * Handles saving the three-view image and triggering auto-crop after successful download.
     */
    private suspend fun handleSuccessfulImageData(taskId: String, imageData: ByteArray) {
        // Save the three-view image
        taskRepository.saveThreeViewImage(taskId, imageData)

        // Trigger auto-crop
        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
        if (bitmap != null) {
            val cropResult = cropperService.autoCrop(bitmap)
            when (cropResult) {
                is com.example.sketchto3view.domain.model.CropResult.Success -> {
                    taskRepository.saveCroppedImages(taskId, cropResult.images)
                    taskRepository.updateTaskStatus(taskId, TaskStatus.COMPLETED)
                    notificationHelper.showCompletionNotification(taskId)
                }
                is com.example.sketchto3view.domain.model.CropResult.DetectionFailed -> {
                    // Auto-crop failed but generation succeeded;
                    // mark as completed so user can manually crop
                    taskRepository.updateTaskStatus(taskId, TaskStatus.COMPLETED)
                    notificationHelper.showCompletionNotification(taskId)
                }
            }
        } else {
            // Could not decode the generated image
            taskRepository.updateTaskStatus(
                taskId,
                TaskStatus.FAILED,
                "Failed to decode generated three-view image"
            )
        }
    }
}
