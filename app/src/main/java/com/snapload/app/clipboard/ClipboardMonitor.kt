package com.snapload.app.clipboard

import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import com.snapload.app.utils.Constants
import com.snapload.app.utils.Extensions.detectPlatform
import com.snapload.app.utils.Extensions.extractUrls
import com.snapload.app.utils.Extensions.isValidUrl

/**
 * يراقب الـ clipboard عند عودة التطبيق للأمام (onResume)
 * إذا وجد رابط مدعوم → Snackbar مع زر "تحليل" و"تجاهل"
 * يتذكر الروابط التي تجاهلها المستخدم
 */
class ClipboardMonitor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)

    private val ignoredKey = "ignored_clipboard_urls"

    interface ClipboardListener {
        fun onSupportedUrlDetected(url: String, platform: String)
    }

    /**
     * يستدعى في onResume لفحص الحافظة
     */
    fun checkClipboard(listener: ClipboardListener) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip ?: return
        if (clip.itemCount == 0) return

        val text = clip.getItemAt(0).coerceToText(context).toString().trim()
        val urls = text.extractUrls()
        if (urls.isEmpty()) return

        val url = urls.first()
        if (!url.isValidUrl()) return

        val platform = url.detectPlatform()
        if (!isSupportedPlatform(url)) return
        if (isIgnored(url)) return

        listener.onSupportedUrlDetected(url, platform)
    }

    /** احفظ الرابط في قائمة التجاهل */
    fun ignoreUrl(url: String) {
        val set = getIgnoredUrls().toMutableSet()
        set.add(url)
        // نحتفظ بآخر 100 فقط
        val trimmed = if (set.size > 100) set.drop(set.size - 100).toSet() else set
        prefs.edit().putStringSet(ignoredKey, trimmed).apply()
    }

    fun clearIgnoredUrls() {
        prefs.edit().remove(ignoredKey).apply()
    }

    private fun isIgnored(url: String): Boolean = getIgnoredUrls().contains(url)

    private fun getIgnoredUrls(): Set<String> =
        prefs.getStringSet(ignoredKey, emptySet()) ?: emptySet()

    private fun isSupportedPlatform(url: String): Boolean {
        return Constants.SUPPORTED_DOMAINS.any { domain -> url.contains(domain) }
    }
}
