package com.example.sketchto3view.domain.service

import com.example.sketchto3view.domain.model.GenerationRequest
import com.example.sketchto3view.domain.model.GenerationResult

/**
 * Abstract interface for image generation services.
 * Allows different generation service providers to be integrated
 * without modifying core application logic.
 */
interface ImageGenerationApi {
    suspend fun generateThreeView(request: GenerationRequest): GenerationResult

    /**
     * Downloads an image from a URL and returns the bytes.
     * Used for background-resilient download flow: the URL is saved first,
     * then downloaded separately (possibly after app resume).
     *
     * @return image bytes if successful, null if download failed
     */
    suspend fun downloadImageFromUrl(url: String): ByteArray?
}
