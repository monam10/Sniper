package com.snapload.app.utils

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

object FileUtils {

    private const val PROVIDER_AUTHORITY = "com.snapload.app.fileprovider"

    // ──────────────────── Paths ────────────────────

    fun getDefaultDownloadPath(): String {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "SnapLoad"
        )
        return dir.absolutePath
    }

    fun createDownloadDirectory(): File {
        val dir = File(getDefaultDownloadPath())
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // ──────────────────── File Info ────────────────────

    fun getFileSize(path: String): Long {
        return try { File(path).length() } catch (e: Exception) { 0L }
    }

    fun getMimeType(ext: String): String {
        return when (ext.lowercase()) {
            "mp4", "mkv", "webm", "avi", "mov", "flv" -> "video/*"
            "mp3", "m4a", "ogg", "opus", "flac", "wav", "aac" -> "audio/*"
            else -> "*/*"
        }
    }

    // ──────────────────── Operations ────────────────────

    fun deleteFile(path: String): Boolean {
        return try {
            val file = File(path)
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            false
        }
    }

    fun openFile(context: Context, path: String, ext: String) {
        try {
            val file = File(path)
            if (!file.exists()) {
                context.showToast("الملف غير موجود")
                return
            }
            val uri = getFileUri(context, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(ext))
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(intent, "فتح باستخدام"))
        } catch (e: Exception) {
            context.showToast("لا يوجد تطبيق لفتح هذا الملف")
        }
    }

    fun shareFile(context: Context, path: String, ext: String) {
        try {
            val file = File(path)
            if (!file.exists()) {
                context.showToast("الملف غير موجود")
                return
            }
            val uri = getFileUri(context, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = getMimeType(ext)
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(Intent.createChooser(intent, "مشاركة باستخدام"))
        } catch (e: Exception) {
            context.showToast("فشل في مشاركة الملف")
        }
    }

    fun scanMediaFile(context: Context, path: String, mimeType: String? = null) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(path),
            mimeType?.let { arrayOf(it) },
            null
        )
    }

    // ──────────────────── URI Helper ────────────────────

    private fun getFileUri(context: Context, file: File): Uri {
        return try {
            FileProvider.getUriForFile(context, PROVIDER_AUTHORITY, file)
        } catch (e: Exception) {
            Uri.fromFile(file)
        }
    }
}
