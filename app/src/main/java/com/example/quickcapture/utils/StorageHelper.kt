package com.example.quickcapture.utils

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StorageHelper {

    private const val DIR_NAME = "private_captures"

    private fun getStorageDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getOutputFile(context: Context, isVideo: Boolean): File {
        val dir = getStorageDir(context)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val extension = if (isVideo) ".mp4" else ".m4a"
        val prefix = if (isVideo) "VID_" else "AUD_"
        return File(dir, "$prefix$timestamp$extension")
    }

    fun getPrivateCaptures(context: Context): List<File> {
        val dir = getStorageDir(context)
        return dir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun listCaptures(context: Context): List<File> = getPrivateCaptures(context)

    fun deleteCapture(file: File): Boolean {
        return file.exists() && file.delete()
    }
}
