package com.snapload.app.utils

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File

/**
 * فتح الملف بعد التحميل + مشاركته
 */
object FileOpenHelper {

    private const val FILE_PROVIDER_AUTHORITY = "com.snapload.app.fileprovider"

    /** فتح الملف في التطبيق المناسب */
    fun openFile(context: Context, filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) return false

            val uri = getFileUri(context, file)
            val mime = getMimeType(filePath)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val chooser = Intent.createChooser(intent, "فتح باستخدام")
            chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** مشاركة الملف مع أي تطبيق */
    fun shareFile(context: Context, filePath: String, title: String = ""): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) return false

            val uri = getFileUri(context, file)
            val mime = getMimeType(filePath)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, title)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val chooser = Intent.createChooser(intent, "مشاركة عبر")
            chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** الحصول على Uri صالح للـ FileProvider */
    fun getFileUri(context: Context, file: File): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
        } else {
            Uri.fromFile(file)
        }
    }

    /** تحديد MIME type من امتداد الملف */
    fun getMimeType(filePath: String): String {
        val ext = filePath.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp4", "mkv", "webm", "avi", "mov" -> "video/$ext"
            "mp3"        -> "audio/mpeg"
            "m4a"        -> "audio/mp4"
            "aac"        -> "audio/aac"
            "ogg"        -> "audio/ogg"
            "opus"       -> "audio/opus"
            "flac"       -> "audio/flac"
            "wav"        -> "audio/wav"
            else         -> MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(ext) ?: "*/*"
        }
    }

    /** تسجيل الملف في MediaStore لظهوره في معرض الصور/الفيديوهات */
    fun scanMediaFile(context: Context, filePath: String, onScanned: ((String) -> Unit)? = null) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(filePath),
            null
        ) { path, _ -> onScanned?.invoke(path) }
    }
}
