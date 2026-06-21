package com.example.sketchto3view.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.sketchto3view.data.local.TaskDao
import com.example.sketchto3view.data.local.TaskEntity
import com.example.sketchto3view.domain.model.GenerationTask
import com.example.sketchto3view.domain.model.TaskStatus
import com.example.sketchto3view.domain.repository.TaskRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TaskRepository using Room for metadata and file system for images.
 *
 * File storage structure:
 * - {app_files_dir}/tasks/{taskId}/source.png
 * - {app_files_dir}/tasks/{taskId}/three_view.png
 * - {app_files_dir}/tasks/{taskId}/crop_front.png
 * - {app_files_dir}/tasks/{taskId}/crop_side.png
 * - {app_files_dir}/tasks/{taskId}/crop_top.png
 */
@Singleton
class FileBasedTaskRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val gson: Gson
) : TaskRepository {

    companion object {
        private const val TASKS_DIR = "tasks"
        private const val SOURCE_IMAGE_NAME = "source.png"
        private const val THREE_VIEW_IMAGE_NAME = "three_view.png"
        private val CROP_IMAGE_NAMES = listOf("crop_front.png", "crop_side.png", "crop_top.png")
    }

    /**
     * Returns the base directory for all tasks.
     */
    private fun getTasksBaseDir(): File {
        return File(context.filesDir, TASKS_DIR)
    }

    /**
     * Returns the directory for a specific task.
     */
    private fun getTaskDir(taskId: String): File {
        return File(getTasksBaseDir(), taskId)
    }

    override suspend fun createTask(sourceImageUri: Uri): GenerationTask = withContext(Dispatchers.IO) {
        val taskId = UUID.randomUUID().toString()
        val taskDir = getTaskDir(taskId)
        taskDir.mkdirs()

        // Copy source image to app-specific directory
        val sourceFile = File(taskDir, SOURCE_IMAGE_NAME)
        context.contentResolver.openInputStream(sourceImageUri)?.use { inputStream ->
            FileOutputStream(sourceFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw IllegalArgumentException("Cannot open source image URI: $sourceImageUri")

        val createdAt = System.currentTimeMillis()
        val entity = TaskEntity(
            id = taskId,
            sourceImagePath = sourceFile.absolutePath,
            status = TaskStatus.QUEUED.name,
            threeViewImagePath = null,
            croppedImagePaths = null,
            errorMessage = null,
            createdAt = createdAt
        )
        taskDao.insertTask(entity)

        GenerationTask(
            id = taskId,
            sourceImagePath = sourceFile.absolutePath,
            status = TaskStatus.QUEUED,
            createdAt = createdAt
        )
    }

    override suspend fun getTask(taskId: String): GenerationTask? {
        return taskDao.getTask(taskId)?.toDomain()
    }

    override fun getAllTasks(): Flow<List<GenerationTask>> {
        return taskDao.getAllTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun updateTaskStatus(taskId: String, status: TaskStatus, error: String?) {
        taskDao.updateStatus(taskId, status.name, error)
    }

    override suspend fun saveThreeViewImage(taskId: String, imageData: ByteArray): String =
        withContext(Dispatchers.IO) {
            val taskDir = getTaskDir(taskId)
            taskDir.mkdirs()

            val threeViewFile = File(taskDir, THREE_VIEW_IMAGE_NAME)
            FileOutputStream(threeViewFile).use { outputStream ->
                outputStream.write(imageData)
            }

            val path = threeViewFile.absolutePath
            taskDao.updateThreeViewImagePath(taskId, path)
            path
        }

    override suspend fun saveCroppedImages(taskId: String, images: List<Bitmap>): List<String> =
        withContext(Dispatchers.IO) {
            require(images.size == CROP_IMAGE_NAMES.size) {
                "Expected exactly ${CROP_IMAGE_NAMES.size} cropped images, got ${images.size}"
            }

            val taskDir = getTaskDir(taskId)
            taskDir.mkdirs()

            // Delete old cropped images if they exist
            CROP_IMAGE_NAMES.forEach { name ->
                File(taskDir, name).let { if (it.exists()) it.delete() }
            }

            val paths = images.zip(CROP_IMAGE_NAMES).map { (bitmap, fileName) ->
                val file = File(taskDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                file.absolutePath
            }

            val pathsJson = gson.toJson(paths)
            taskDao.updateCroppedImagePaths(taskId, pathsJson)
            paths
        }

    override suspend fun deleteTask(taskId: String): Unit = withContext(Dispatchers.IO) {
        // Delete all associated files
        val taskDir = getTaskDir(taskId)
        if (taskDir.exists()) {
            taskDir.deleteRecursively()
        }

        // Delete database record
        taskDao.deleteTaskById(taskId)
    }

    private fun TaskEntity.toDomain(): GenerationTask {
        val croppedPaths: List<String> = if (!croppedImagePaths.isNullOrEmpty()) {
            gson.fromJson(croppedImagePaths, object : TypeToken<List<String>>() {}.type)
        } else {
            emptyList()
        }
        return GenerationTask(
            id = id,
            sourceImagePath = sourceImagePath,
            status = TaskStatus.valueOf(status),
            threeViewImagePath = threeViewImagePath,
            croppedImagePaths = croppedPaths,
            errorMessage = errorMessage,
            createdAt = createdAt
        )
    }
}
