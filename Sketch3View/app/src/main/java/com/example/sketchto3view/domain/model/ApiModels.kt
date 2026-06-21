package com.example.sketchto3view.domain.model

import android.net.Uri

/**
 * Request model for the image generation API.
 */
data class GenerationRequest(
    val sourceImage: ByteArray,
    val prompt: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as GenerationRequest
        if (!sourceImage.contentEquals(other.sourceImage)) return false
        if (prompt != other.prompt) return false
        return true
    }

    override fun hashCode(): Int {
        var result = sourceImage.contentHashCode()
        result = 31 * result + prompt.hashCode()
        return result
    }
}

/**
 * Result from the image generation API.
 */
sealed class GenerationResult {
    data class Success(val imageData: ByteArray) : GenerationResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Success
            return imageData.contentEquals(other.imageData)
        }

        override fun hashCode(): Int = imageData.contentHashCode()
    }

    /**
     * API returned a URL that needs to be downloaded separately.
     * Used for background-resilient flow: save URL first, then download.
     */
    data class SuccessUrl(val imageUrl: String) : GenerationResult()

    data class Error(val code: Int, val message: String) : GenerationResult()
}

/**
 * Result from the cropper service.
 */
sealed class CropResult {
    data class Success(val images: List<android.graphics.Bitmap>) : CropResult()
    data class DetectionFailed(val reason: String) : CropResult()
}

/**
 * Result from the download manager.
 */
sealed class DownloadResult {
    data class Success(val uri: Uri, val path: String) : DownloadResult()
    data class Failure(val message: String) : DownloadResult()
}
