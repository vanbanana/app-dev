package com.example.sketchto3view.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for task operations.
 */
@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTask(taskId: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Query("UPDATE tasks SET status = :status, errorMessage = :error WHERE id = :taskId")
    suspend fun updateStatus(taskId: String, status: String, error: String?)

    @Query("UPDATE tasks SET threeViewImagePath = :path WHERE id = :taskId")
    suspend fun updateThreeViewImagePath(taskId: String, path: String)

    @Query("UPDATE tasks SET croppedImagePaths = :paths WHERE id = :taskId")
    suspend fun updateCroppedImagePaths(taskId: String, paths: String)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("UPDATE tasks SET status = :newStatus, errorMessage = :error WHERE status = :oldStatus")
    suspend fun resetStuckTasks(oldStatus: String, newStatus: String, error: String)

    @Query("UPDATE tasks SET pendingImageUrl = :url WHERE id = :taskId")
    suspend fun updatePendingImageUrl(taskId: String, url: String)

    @Query("SELECT * FROM tasks WHERE pendingImageUrl IS NOT NULL AND status = 'PROCESSING'")
    suspend fun getTasksWithPendingDownload(): List<TaskEntity>

    @Query("UPDATE tasks SET pendingImageUrl = NULL WHERE id = :taskId")
    suspend fun clearPendingImageUrl(taskId: String)
}
