package com.example.sketchto3view.domain.usecase

import android.graphics.BitmapFactory
import com.example.sketchto3view.domain.model.CropLine
import com.example.sketchto3view.domain.model.CropResult
import com.example.sketchto3view.domain.model.TaskStatus
import com.example.sketchto3view.domain.repository.TaskRepository
import com.example.sketchto3view.domain.service.CropperService
import java.io.File
import javax.inject.Inject

/**
 * Use case for cropping three-view images into separate views.
 *
 * Supports two modes:
 * - Auto-crop: Automatically detects boundaries between views
 * - Manual crop: Uses user-provided crop lines to split the image
 */
class CropImageUseCase @Inject constructor(
    private val cropperService: CropperService,
    private val taskRepository: TaskRepository
) {
    /**
     * Automatically crops the three-view image for the given task.
     *
     * Loads the three-view image from the task, calls the cropper service's
     * auto-detection algorithm, and saves the resulting cropped images.
     *
     * @param taskId The ID of the task whose three-view image should be cropped
     * @return CropResult indicating success (with 3 images) or detection failure
     */
    suspend fun autoCrop(taskId: String): CropResult {
        val task = taskRepository.getTask(taskId)
            ?: return CropResult.DetectionFailed("Task not found: $taskId")

        val threeViewPath = task.threeViewImagePath
            ?: return CropResult.DetectionFailed("No three-view image available for task: $taskId")

        val imageFile = File(threeViewPath)
        if (!imageFile.exists()) {
            return CropResult.DetectionFailed("Three-view image file not found: $threeViewPath")
        }

        val bitmap = BitmapFactory.decodeFile(threeViewPath)
            ?: return CropResult.DetectionFailed("Failed to decode three-view image")

        val cropResult = cropperService.autoCrop(bitmap)

        if (cropResult is CropResult.Success) {
            taskRepository.saveCroppedImages(taskId, cropResult.images)
        }

        return cropResult
    }

    /**
     * Manually crops the three-view image using user-provided crop lines.
     *
     * Loads the three-view image, applies the specified crop lines to split it
     * into three views, and saves the results (replacing any previously cropped images).
     *
     * @param taskId The ID of the task whose three-view image should be cropped
     * @param cropLines The list of crop lines defining where to split the image
     * @return CropResult indicating success (with 3 images) or failure
     */
    suspend fun manualCrop(taskId: String, cropLines: List<CropLine>): CropResult {
        val task = taskRepository.getTask(taskId)
            ?: return CropResult.DetectionFailed("Task not found: $taskId")

        val threeViewPath = task.threeViewImagePath
            ?: return CropResult.DetectionFailed("No three-view image available for task: $taskId")

        val imageFile = File(threeViewPath)
        if (!imageFile.exists()) {
            return CropResult.DetectionFailed("Three-view image file not found: $threeViewPath")
        }

        val bitmap = BitmapFactory.decodeFile(threeViewPath)
            ?: return CropResult.DetectionFailed("Failed to decode three-view image")

        val cropResult = cropperService.manualCrop(bitmap, cropLines)

        if (cropResult is CropResult.Success) {
            // Save results, replacing any old cropped images
            taskRepository.saveCroppedImages(taskId, cropResult.images)
        }

        return cropResult
    }
}
