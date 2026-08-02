package com.snapload.app.history

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.snapload.app.data.model.VideoInfo
import com.snapload.app.utils.Constants

/**
 * يحفظ آخر 50 رابط تم تحليلها في SharedPreferences
 */
class HistoryManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("snapload_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val historyKey = "video_history"
    private val maxItems = 50

    data class HistoryItem(
        val url: String,
        val title: String,
        val thumbnail: String,
        val platform: String,
        val addedAt: Long = System.currentTimeMillis()
    )

    fun addToHistory(url: String, videoInfo: VideoInfo) {
        val list = getHistory().toMutableList()
        // أزل إذا كان موجوداً مسبقاً
        list.removeAll { it.url == url }
        // أضف في البداية
        list.add(
            0, HistoryItem(
                url = url,
                title = videoInfo.title,
                thumbnail = videoInfo.thumbnail,
                platform = videoInfo.platform
            )
        )
        // احتفظ بآخر maxItems فقط
        val trimmed = if (list.size > maxItems) list.take(maxItems) else list
        prefs.edit().putString(historyKey, gson.toJson(trimmed)).apply()
    }

    fun getHistory(): List<HistoryItem> {
        val json = prefs.getString(historyKey, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<HistoryItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearHistory() {
        prefs.edit().remove(historyKey).apply()
    }

    fun removeItem(url: String) {
        val list = getHistory().toMutableList()
        list.removeAll { it.url == url }
        prefs.edit().putString(historyKey, gson.toJson(list)).apply()
    }

    fun hasHistory(): Boolean = getHistory().isNotEmpty()
}
