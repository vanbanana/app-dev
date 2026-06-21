package com.example.sketchto3view.domain.repository

import android.graphics.Bitmap
import android.net.Uri
import com.example.sketchto3view.domain.model.GenerationTask
import com.example.sketchto3view.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing generation tasks and associated images.
 */
interface TaskRepository {
    suspend fun createTask(sourceImageUri: Uri): GenerationTask
    suspend fun getTask(taskId: String): GenerationTask?
    fun getAllTasks(): Flow<List<GenerationTask>>
    suspend fun updateTaskStatus(taskId: String, status: TaskStatus, error: String? = null)
    suspend fun saveThreeViewImage(taskId: String, imageData: ByteArray): String
    suspend fun saveCroppedImages(taskId: String, images: List<Bitmap>): List<String>
    suspend fun deleteTask(taskId: String)
}
