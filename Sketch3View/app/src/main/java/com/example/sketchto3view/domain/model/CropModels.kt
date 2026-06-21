package com.example.sketchto3view.domain.model

/**
 * Represents a crop line position on the three-view image.
 */
data class CropLine(
    val orientation: Orientation,
    val position: Float // 0.0 to 1.0 relative to image dimension
)

enum class Orientation {
    VERTICAL,
    HORIZONTAL
}

/**
 * Represents a cropped region within the three-view image.
 */
data class CropRegion(
    val label: String, // "front", "side", "top"
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int
)
