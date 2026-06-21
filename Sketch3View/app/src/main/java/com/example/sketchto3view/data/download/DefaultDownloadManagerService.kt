package com.example.sketchto3view.data.download

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.sketchto3view.domain.service.DownloadManagerService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of DownloadManagerService using MediaStore API for Android 10+
 * and direct file access for older versions.
 */
@Singleton
class DefaultDownloadManagerService @Inject constructor(
    @ApplicationContext private val context: Context
) : DownloadManagerService {

    override suspend fun saveToDownloads(fileName: String, imageData: ByteArray): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToDownloadsMediaStore(fileName, imageData)
        } else {
            saveToDownloadsLegacy(fileName, imageData)
        }
    }

    override suspend fun saveAsZip(fileName: String, images: Map<String, ByteArray>): Uri {
        val zipData = createZipInMemory(images)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveZipToDownloadsMediaStore(fileName, zipData)
        } else {
            saveZipToDownloadsLegacy(fileName, zipData)
        }
    }

    /**
     * Save an image to Downloads using MediaStore API (Android 10+).
     */
    private fun saveToDownloadsMediaStore(fileName: String, imageData: ByteArray): Uri {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(fileName))
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), contentValues)
            ?: throw IOException("Failed to create MediaStore entry for file: $fileName")

        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(imageData)
                outputStream.flush()
            } ?: throw IOException("Failed to open output stream for URI: $uri")
        } catch (e: IOException) {
            // Clean up the created entry on failure
            resolver.delete(uri, null, null)
            throw IOException("Failed to write image data to downloads: ${e.message}", e)
        }

        return uri
    }

    /**
     * Save an image to Downloads using direct file access (Android 9 and below).
     */
    private fun saveToDownloadsLegacy(fileName: String, imageData: ByteArray): Uri {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val file = File(downloadsDir, fileName)
        try {
            FileOutputStream(file).use { outputStream ->
                outputStream.write(imageData)
                outputStream.flush()
            }
        } catch (e: IOException) {
            throw IOException("Failed to write image to downloads directory: ${e.message}", e)
        }

        return Uri.fromFile(file)
    }

    /**
     * Save a ZIP file to Downloads using MediaStore API (Android 10+).
     */
    private fun saveZipToDownloadsMediaStore(fileName: String, zipData: ByteArray): Uri {
        val zipFileName = if (fileName.endsWith(".zip", ignoreCase = true)) fileName else "$fileName.zip"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, zipFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), contentValues)
            ?: throw IOException("Failed to create MediaStore entry for ZIP file: $zipFileName")

        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(zipData)
                outputStream.flush()
            } ?: throw IOException("Failed to open output stream for ZIP URI: $uri")
        } catch (e: IOException) {
            // Clean up the created entry on failure
            resolver.delete(uri, null, null)
            throw IOException("Failed to write ZIP data to downloads: ${e.message}", e)
        }

        return uri
    }

    /**
     * Save a ZIP file to Downloads using direct file access (Android 9 and below).
     */
    private fun saveZipToDownloadsLegacy(fileName: String, zipData: ByteArray): Uri {
        val zipFileName = if (fileName.endsWith(".zip", ignoreCase = true)) fileName else "$fileName.zip"

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val file = File(downloadsDir, zipFileName)
        try {
            FileOutputStream(file).use { outputStream ->
                outputStream.write(zipData)
                outputStream.flush()
            }
        } catch (e: IOException) {
            throw IOException("Failed to write ZIP to downloads directory: ${e.message}", e)
        }

        return Uri.fromFile(file)
    }

    /**
     * Create a ZIP file in memory containing all provided images.
     *
     * @param images Map of entry names to image byte arrays
     * @return The ZIP file as a byte array
     */
    private fun createZipInMemory(images: Map<String, ByteArray>): ByteArray {
        if (images.isEmpty()) {
            throw IllegalArgumentException("Cannot create ZIP with no images")
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        try {
            ZipOutputStream(byteArrayOutputStream).use { zipOutputStream ->
                for ((entryName, imageData) in images) {
                    val zipEntry = ZipEntry(entryName)
                    zipOutputStream.putNextEntry(zipEntry)
                    zipOutputStream.write(imageData)
                    zipOutputStream.closeEntry()
                }
            }
        } catch (e: IOException) {
            throw IOException("Failed to create ZIP file in memory: ${e.message}", e)
        }

        return byteArrayOutputStream.toByteArray()
    }

    /**
     * Determine MIME type based on file extension.
     */
    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".png", ignoreCase = true) -> "image/png"
            fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            fileName.endsWith(".webp", ignoreCase = true) -> "image/webp"
            else -> "image/png" // Default to PNG
        }
    }

    /**
     * Share a file via Android's share sheet (ACTION_SEND).
     * This will show all available share targets including WeChat if installed.
     */
    override fun shareFile(uri: Uri, mimeType: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooserIntent = Intent.createChooser(shareIntent, "分享到...")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }
}
