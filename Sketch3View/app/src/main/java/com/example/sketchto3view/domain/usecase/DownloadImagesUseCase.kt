package com.example.sketchto3view.domain.usecase

import com.example.sketchto3view.domain.model.DownloadResult
import com.example.sketchto3view.domain.model.TaskStatus
import com.example.sketchto3view.domain.repository.TaskRepository
import com.example.sketchto3view.domain.service.DownloadManagerService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Use case for downloading/exporting cropped view images.
 *
 * Supports four download modes:
 * - downloadAsZip: Packages all 3 cropped views into a single ZIP file
 * - downloadSingle: Saves one specific view image to downloads
 * - downloadAll: Concurrently saves all 3 view images to downloads
 * - downloadAllTasks: Packages ALL completed tasks into one ZIP file
 */
class DownloadImagesUseCase @Inject constructor(
    private val downloadManager: DownloadManagerService,
    private val taskRepository: TaskRepository
) {
    companion object {
        private val VIEW_LABELS = listOf("front", "side", "top")
    }

    /**
     * Downloads all 3 cropped view images packaged as a single ZIP file.
     *
     * @param taskId The ID of the task whose cropped images should be downloaded
     * @return DownloadResult indicating success with the ZIP file URI/path, or failure
     */
    suspend fun downloadAsZip(taskId: String): DownloadResult {
        return try {
            val task = taskRepository.getTask(taskId)
                ?: return DownloadResult.Failure("Task not found: $taskId")

            val croppedPaths = task.croppedImagePaths
            if (croppedPaths.size != 3) {
                return DownloadResult.Failure("Expected 3 cropped images, found ${croppedPaths.size}")
            }

            // Read all cropped image files into a map
            val images = mutableMapOf<String, ByteArray>()
            croppedPaths.forEachIndexed { index, path ->
                val file = File(path)
                if (!file.exists()) {
                    return DownloadResult.Failure("Cropped image file not found: $path")
                }
                val label = VIEW_LABELS.getOrElse(index) { "view_$index" }
                images["${label}.png"] = file.readBytes()
            }

            val zipFileName = "three_view_${taskId.take(8)}.zip"
            val uri = downloadManager.saveAsZip(zipFileName, images)
            DownloadResult.Success(uri = uri, path = uri.toString())
        } catch (e: Exception) {
            DownloadResult.Failure("Failed to create ZIP download: ${e.message}")
        }
    }

    /**
     * Downloads a single cropped view image to the device's downloads directory.
     *
     * @param taskId The ID of the task
     * @param viewIndex The index of the view to download (0=front, 1=side, 2=top)
     * @return DownloadResult indicating success with the file URI/path, or failure
     */
    suspend fun downloadSingle(taskId: String, viewIndex: Int): DownloadResult {
        return try {
            if (viewIndex !in 0..2) {
                return DownloadResult.Failure("Invalid view index: $viewIndex. Must be 0, 1, or 2.")
            }

            val task = taskRepository.getTask(taskId)
                ?: return DownloadResult.Failure("Task not found: $taskId")

            val croppedPaths = task.croppedImagePaths
            if (viewIndex >= croppedPaths.size) {
                return DownloadResult.Failure("View index $viewIndex not available. Only ${croppedPaths.size} cropped images exist.")
            }

            val path = croppedPaths[viewIndex]
            val file = File(path)
            if (!file.exists()) {
                return DownloadResult.Failure("Cropped image file not found: $path")
            }

            val imageData = file.readBytes()
            val label = VIEW_LABELS.getOrElse(viewIndex) { "view_$viewIndex" }
            val fileName = "${label}_${taskId.take(8)}.png"
            val uri = downloadManager.saveToDownloads(fileName, imageData)
            DownloadResult.Success(uri = uri, path = uri.toString())
        } catch (e: Exception) {
            DownloadResult.Failure("Failed to download image: ${e.message}")
        }
    }

    /**
     * Concurrently downloads all 3 cropped view images to the device's downloads directory.
     *
     * @param taskId The ID of the task whose cropped images should be downloaded
     * @return List of DownloadResult for each view (front, side, top)
     */
    suspend fun downloadAll(taskId: String): List<DownloadResult> = coroutineScope {
        val task = taskRepository.getTask(taskId)
            ?: return@coroutineScope listOf(DownloadResult.Failure("Task not found: $taskId"))

        val croppedPaths = task.croppedImagePaths
        if (croppedPaths.size != 3) {
            return@coroutineScope listOf(
                DownloadResult.Failure("Expected 3 cropped images, found ${croppedPaths.size}")
            )
        }

        // Launch all downloads concurrently
        val deferredResults = croppedPaths.mapIndexed { index, path ->
            async {
                try {
                    val file = File(path)
                    if (!file.exists()) {
                        return@async DownloadResult.Failure("Cropped image file not found: $path")
                    }

                    val imageData = file.readBytes()
                    val label = VIEW_LABELS.getOrElse(index) { "view_$index" }
                    val fileName = "${label}_${taskId.take(8)}.png"
                    val uri = downloadManager.saveToDownloads(fileName, imageData)
                    DownloadResult.Success(uri = uri, path = uri.toString()) as DownloadResult
                } catch (e: Exception) {
                    DownloadResult.Failure("Failed to download view $index: ${e.message}") as DownloadResult
                }
            }
        }

        deferredResults.map { it.await() }
    }

    /**
     * Downloads selected tasks packaged into a single ZIP file.
     *
     * @param taskIds Set of task IDs to download
     * @return DownloadResult indicating success with the ZIP file URI/path, or failure
     */
    suspend fun downloadSelectedTasks(taskIds: Set<String>): DownloadResult {
        return try {
            if (taskIds.isEmpty()) {
                return DownloadResult.Failure("No tasks selected for download")
            }

            val allTasks = taskRepository.getAllTasks().first()
            val selectedTasks = allTasks.filter { it.id in taskIds && it.status == TaskStatus.COMPLETED }

            if (selectedTasks.isEmpty()) {
                return DownloadResult.Failure("No completed tasks found in selection")
            }

            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val dateStr = dateFormat.format(Date())
            val batchFolderName = "sketch3view_selected_$dateStr"

            val zipEntries = mutableMapOf<String, ByteArray>()

            selectedTasks.forEachIndexed { index, task ->
                val croppedPaths = task.croppedImagePaths
                if (croppedPaths.isEmpty()) return@forEachIndexed

                val sourceFileName = task.sourceImagePath
                    .substringAfterLast("/")
                    .substringBeforeLast(".")
                    .ifEmpty { "task_${task.id.take(6)}" }

                val subFolderName = "task${index + 1}_$sourceFileName"

                croppedPaths.forEachIndexed { viewIndex, path ->
                    val file = File(path)
                    if (file.exists()) {
                        val label = VIEW_LABELS.getOrElse(viewIndex) { "view_$viewIndex" }
                        val entryName = "$batchFolderName/$subFolderName/${label}.png"
                        zipEntries[entryName] = file.readBytes()
                    }
                }
            }

            if (zipEntries.isEmpty()) {
                return DownloadResult.Failure("No cropped image files found for selected tasks")
            }

            val zipFileName = "sketch3view_selected_${dateStr}_${selectedTasks.size}tasks.zip"
            val uri = downloadManager.saveAsZip(zipFileName, zipEntries)
            DownloadResult.Success(uri = uri, path = uri.toString())
        } catch (e: Exception) {
            DownloadResult.Failure("Failed to create selected ZIP download: ${e.message}")
        }
    }

    /**
     * Downloads ALL completed tasks packaged into a single ZIP file.
     *
     * ZIP structure:
     * sketch3view_batch_{date}/
     * ├── task1_source_filename/
     * │   ├── front.png
     * │   ├── side.png
     * │   └── top.png
     * ├── task2_source_filename/
     * │   ├── front.png
     * │   ├── side.png
     * │   └── top.png
     * └── ...
     *
     * @return DownloadResult indicating success with the ZIP file URI/path, or failure
     */
    suspend fun downloadAllTasks(): DownloadResult {
        return try {
            // Get all tasks and filter to completed ones
            val allTasks = taskRepository.getAllTasks().first()
            val completedTasks = allTasks.filter { it.status == TaskStatus.COMPLETED }

            if (completedTasks.isEmpty()) {
                return DownloadResult.Failure("No completed tasks to download")
            }

            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val dateStr = dateFormat.format(Date())
            val batchFolderName = "sketch3view_batch_$dateStr"

            // Build the ZIP entries map: "folder/filename.png" -> bytes
            val zipEntries = mutableMapOf<String, ByteArray>()

            completedTasks.forEachIndexed { index, task ->
                val croppedPaths = task.croppedImagePaths
                if (croppedPaths.isEmpty()) return@forEachIndexed

                // Use source image filename (without extension) as subfolder name
                val sourceFileName = task.sourceImagePath
                    .substringAfterLast("/")
                    .substringBeforeLast(".")
                    .ifEmpty { "task_${task.id.take(6)}" }

                val subFolderName = "task${index + 1}_$sourceFileName"

                croppedPaths.forEachIndexed { viewIndex, path ->
                    val file = File(path)
                    if (file.exists()) {
                        val label = VIEW_LABELS.getOrElse(viewIndex) { "view_$viewIndex" }
                        val entryName = "$batchFolderName/$subFolderName/${label}.png"
                        zipEntries[entryName] = file.readBytes()
                    }
                }
            }

            if (zipEntries.isEmpty()) {
                return DownloadResult.Failure("No cropped image files found for completed tasks")
            }

            val zipFileName = "sketch3view_batch_${dateStr}_${completedTasks.size}tasks.zip"
            val uri = downloadManager.saveAsZip(zipFileName, zipEntries)
            DownloadResult.Success(uri = uri, path = uri.toString())
        } catch (e: Exception) {
            DownloadResult.Failure("Failed to create batch ZIP download: ${e.message}")
        }
    }
}
