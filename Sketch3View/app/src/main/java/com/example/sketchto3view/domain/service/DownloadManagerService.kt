package com.example.sketchto3view.domain.service

import android.net.Uri

/**
 * Service interface for downloading/exporting images to shared storage.
 */
interface DownloadManagerService {
    suspend fun saveToDownloads(fileName: String, imageData: ByteArray): Uri
    suspend fun saveAsZip(fileName: String, images: Map<String, ByteArray>): Uri
    fun shareFile(uri: Uri, mimeType: String)
}
