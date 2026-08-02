package com.snapload.app.storage

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Environment
import com.snapload.app.utils.Constants
import java.io.File

/**
 * يدير تحديد مجلد الحفظ باستخدام SAF أو مجلد Downloads الافتراضي
 */
class StorageManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)

    /** يُرجع مسار مجلد الحفظ الحالي */
    fun getDownloadPath(): String {
        val saved = prefs.getString(Constants.PREF_DOWNLOAD_PATH, null)
        return saved ?: getDefaultDownloadPath()
    }

    /** يحفظ مجلد الحفظ المختار عبر SAF */
    fun saveDownloadPath(path: String) {
        prefs.edit().putString(Constants.PREF_DOWNLOAD_PATH, path).apply()
    }

    /** يحفظ URI من SAF (للأجهزة الحديثة) */
    fun saveDownloadUri(uri: Uri) {
        prefs.edit().putString("download_uri", uri.toString()).apply()
        // أيضاً احفظ مسار readable
        prefs.edit().putString(Constants.PREF_DOWNLOAD_PATH, uri.path ?: getDefaultDownloadPath()).apply()
        // امنح صلاحية دائمة
        try {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun getSavedUri(): Uri? {
        val uriStr = prefs.getString("download_uri", null) ?: return null
        return Uri.parse(uriStr)
    }

    /** الحصول على المسار الافتراضي */
    fun getDefaultDownloadPath(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ — استخدم MediaStore (لا حاجة لمسار مباشر)
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .absolutePath + File.separator + "SnapLoad"
        } else {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .absolutePath + File.separator + "SnapLoad"
        }
    }

    /** ينشئ مجلد الحفظ إذا لم يكن موجوداً */
    fun ensureDownloadDirectory(): Boolean {
        return try {
            val dir = File(getDownloadPath())
            if (!dir.exists()) dir.mkdirs() else true
        } catch (e: Exception) {
            false
        }
    }

    /** يحسب حجم مجلد التنزيلات */
    fun getUsedSpace(): Long {
        return try {
            val dir = File(getDownloadPath())
            if (!dir.exists()) 0L
            else dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } catch (e: Exception) {
            0L
        }
    }

    /** يحذف الملفات المؤقتة */
    fun clearCache(): Long {
        var freed = 0L
        try {
            val cacheDir = context.cacheDir
            cacheDir.walkTopDown().filter { it.isFile }.forEach {
                freed += it.length()
                it.delete()
            }
        } catch (e: Exception) {
            // ignore
        }
        return freed
    }
}
