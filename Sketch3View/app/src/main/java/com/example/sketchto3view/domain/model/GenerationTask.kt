package com.example.sketchto3view.domain.model

/**
 * Core domain model representing a generation task.
 */
data class GenerationTask(
    val id: String,
    val sourceImagePath: String,
    val status: TaskStatus,
    val threeViewImagePath: String? = null,
    val croppedImagePaths: List<String> = emptyList(),
    val errorMessage: String? = null,
    val createdAt: Long,
    val queuePosition: Int? = null
)

enum class TaskStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED
}
