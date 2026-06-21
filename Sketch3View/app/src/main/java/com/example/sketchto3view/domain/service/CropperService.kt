package com.example.sketchto3view.domain.service

import android.graphics.Bitmap
import com.example.sketchto3view.domain.model.CropLine
import com.example.sketchto3view.domain.model.CropResult

/**
 * Service interface for cropping three-view images into separate views.
 */
interface CropperService {
    suspend fun autoCrop(image: Bitmap): CropResult
    suspend fun manualCrop(image: Bitmap, cropLines: List<CropLine>): CropResult
}
