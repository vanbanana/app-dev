package com.example.sketchto3view.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a generation task in the database.
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val sourceImagePath: String,
    val status: String,
    val threeViewImagePath: String?,
    val croppedImagePaths: String?, // JSON array of file paths
    val errorMessage: String?,
    val createdAt: Long,
    val pendingImageUrl: String? = null // URL waiting to be downloaded (for background resilience)
)
