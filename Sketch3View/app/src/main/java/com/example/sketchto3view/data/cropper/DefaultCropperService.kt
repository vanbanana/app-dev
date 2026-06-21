package com.example.sketchto3view.data.cropper

import android.graphics.Bitmap
import android.graphics.Color
import com.example.sketchto3view.domain.model.CropLine
import com.example.sketchto3view.domain.model.CropResult
import com.example.sketchto3view.domain.model.Orientation
import com.example.sketchto3view.domain.service.CropperService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of CropperService.
 * Uses content-based bounding box detection on white background to split
 * a three-view image into front, side, and top views.
 *
 * Algorithm:
 * 1. Convert to grayscale, threshold at 250 to find "content" pixels
 * 2. Identify content columns (columns with at least one non-white pixel)
 * 3. Group consecutive content columns into content regions
 * 4. Take the 3 largest content regions as the 3 views
 * 5. Crop each region with 5px padding
 */
@Singleton
class DefaultCropperService @Inject constructor() : CropperService {

    companion object {
        /** Minimum image width in pixels to attempt cropping */
        private const val MIN_IMAGE_WIDTH = 30

        /** Minimum image height in pixels to attempt cropping */
        private const val MIN_IMAGE_HEIGHT = 10

        /**
         * Threshold for considering a pixel as "content" (non-white).
         * Any pixel with grayscale value below this is considered content.
         */
        private const val CONTENT_THRESHOLD = 250

        /** Padding in pixels to add around each cropped region */
        private const val CROP_PADDING = 5
    }

    override suspend fun autoCrop(image: Bitmap): CropResult = withContext(Dispatchers.Default) {
        val width = image.width
        val height = image.height

        // Validate minimum size
        if (width < MIN_IMAGE_WIDTH || height < MIN_IMAGE_HEIGHT) {
            return@withContext CropResult.DetectionFailed(
                "Image too small for cropping: ${width}x${height}"
            )
        }

        // Read all pixels at once for performance
        val pixels = IntArray(width * height)
        image.getPixels(pixels, 0, width, 0, 0, width, height)

        // Step 1 & 2: Find content columns (columns that have at least one non-white pixel)
        val contentColumns = findContentColumns(pixels, width, height)

        if (contentColumns.isEmpty()) {
            return@withContext CropResult.DetectionFailed(
                "No content detected in image (entirely white)"
            )
        }

        // Step 3: Group consecutive content columns into content regions
        val contentRegions = groupConsecutiveColumns(contentColumns)

        if (contentRegions.size < 3) {
            // If fewer than 3 regions detected, fall back to equal thirds
            val third = width / 3
            val croppedImages = listOf(
                Bitmap.createBitmap(image, 0, 0, third, height),
                Bitmap.createBitmap(image, third, 0, third, height),
                Bitmap.createBitmap(image, third * 2, 0, width - third * 2, height)
            )
            return@withContext CropResult.Success(croppedImages)
        }

        // Step 4: Take the 3 largest content regions
        val topThreeRegions = contentRegions
            .sortedByDescending { it.width }
            .take(3)
            .sortedBy { it.startX } // Re-sort left to right

        // Step 5: Crop each region with padding, also find vertical content bounds
        val croppedImages = topThreeRegions.map { region ->
            cropRegionWithPadding(image, pixels, width, height, region)
        }

        CropResult.Success(croppedImages)
    }

    override suspend fun manualCrop(image: Bitmap, cropLines: List<CropLine>): CropResult =
        withContext(Dispatchers.Default) {
            val width = image.width
            val height = image.height

            // Filter to only vertical crop lines (for horizontal splitting)
            val verticalLines = cropLines
                .filter { it.orientation == Orientation.VERTICAL }
                .sortedBy { it.position }

            if (verticalLines.size < 2) {
                return@withContext CropResult.DetectionFailed(
                    "Need at least 2 vertical crop lines to split into 3 regions"
                )
            }

            // Convert relative positions (0.0-1.0) to pixel coordinates
            val splitX1 = (verticalLines[0].position * width).toInt().coerceIn(0, width - 1)
            val splitX2 = (verticalLines[1].position * width).toInt().coerceIn(splitX1 + 1, width)

            // Crop into 3 regions
            val images = mutableListOf<Bitmap>()

            // Region 1: left edge to first split
            val width1 = splitX1.coerceAtLeast(1)
            if (width1 > 0) {
                images.add(Bitmap.createBitmap(image, 0, 0, width1, height))
            }

            // Region 2: first split to second split
            val width2 = (splitX2 - splitX1).coerceAtLeast(1)
            images.add(Bitmap.createBitmap(image, splitX1, 0, width2, height))

            // Region 3: second split to right edge
            val width3 = (width - splitX2).coerceAtLeast(1)
            if (width3 > 0) {
                images.add(Bitmap.createBitmap(image, splitX2, 0, width3, height))
            }

            if (images.size != 3) {
                return@withContext CropResult.DetectionFailed(
                    "Manual crop produced ${images.size} regions instead of 3"
                )
            }

            CropResult.Success(images)
        }

    /**
     * Finds all columns that contain at least one content (non-white) pixel.
     * A pixel is considered content if its grayscale value is below CONTENT_THRESHOLD.
     */
    private fun findContentColumns(pixels: IntArray, width: Int, height: Int): Set<Int> {
        val contentColumns = mutableSetOf<Int>()

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = pixels[y * width + x]
                val grayscale = toGrayscale(pixel)
                if (grayscale < CONTENT_THRESHOLD) {
                    contentColumns.add(x)
                    break // This column has content, move to next column
                }
            }
        }

        return contentColumns
    }

    /**
     * Groups consecutive content columns into contiguous regions.
     */
    private fun groupConsecutiveColumns(columns: Set<Int>): List<ContentRegion> {
        if (columns.isEmpty()) return emptyList()

        val sortedColumns = columns.sorted()
        val regions = mutableListOf<ContentRegion>()
        var regionStart = sortedColumns[0]
        var regionEnd = sortedColumns[0]

        for (i in 1 until sortedColumns.size) {
            if (sortedColumns[i] == regionEnd + 1) {
                // Consecutive column, extend current region
                regionEnd = sortedColumns[i]
            } else {
                // Gap found, save current region and start new one
                regions.add(ContentRegion(regionStart, regionEnd))
                regionStart = sortedColumns[i]
                regionEnd = sortedColumns[i]
            }
        }
        // Add the last region
        regions.add(ContentRegion(regionStart, regionEnd))

        return regions
    }

    /**
     * Crops a content region from the image with padding.
     * Also detects vertical content bounds within the region for tighter cropping.
     */
    private fun cropRegionWithPadding(
        image: Bitmap,
        pixels: IntArray,
        imageWidth: Int,
        imageHeight: Int,
        region: ContentRegion
    ): Bitmap {
        // Find vertical content bounds within this region
        var topY = imageHeight
        var bottomY = 0

        for (x in region.startX..region.endX) {
            for (y in 0 until imageHeight) {
                val pixel = pixels[y * imageWidth + x]
                val grayscale = toGrayscale(pixel)
                if (grayscale < CONTENT_THRESHOLD) {
                    if (y < topY) topY = y
                    if (y > bottomY) bottomY = y
                }
            }
        }

        // If no content found vertically (shouldn't happen), use full height
        if (topY >= bottomY) {
            topY = 0
            bottomY = imageHeight - 1
        }

        // Apply padding
        val cropLeft = (region.startX - CROP_PADDING).coerceAtLeast(0)
        val cropRight = (region.endX + CROP_PADDING).coerceAtMost(imageWidth - 1)
        val cropTop = (topY - CROP_PADDING).coerceAtLeast(0)
        val cropBottom = (bottomY + CROP_PADDING).coerceAtMost(imageHeight - 1)

        val cropWidth = (cropRight - cropLeft + 1).coerceAtLeast(1)
        val cropHeight = (cropBottom - cropTop + 1).coerceAtLeast(1)

        return Bitmap.createBitmap(image, cropLeft, cropTop, cropWidth, cropHeight)
    }

    /**
     * Converts a pixel color to grayscale value (0-255).
     * Uses standard luminance formula.
     */
    private fun toGrayscale(pixel: Int): Int {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return ((0.299 * r) + (0.587 * g) + (0.114 * b)).toInt()
    }

    /**
     * Represents a contiguous region of content columns.
     */
    private data class ContentRegion(
        val startX: Int,
        val endX: Int
    ) {
        val width: Int get() = endX - startX + 1
    }
}
