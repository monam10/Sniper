package com.snapload.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.snapload.app.MainActivity
import com.snapload.app.R
import com.snapload.app.utils.Constants

/**
 * ودجت الشاشة الرئيسية:
 * يعرض: آخر تحميل + زر "تحميل جديد" + عدد التحميلات الجارية
 * يُحدَّث كل 30 دقيقة
 */
class DownloadWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_download)

        // فتح التطبيق عند الضغط
        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_root, openIntent)

        // عدد التحميلات الجارية من SharedPreferences
        val prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        val activeCount = prefs.getInt("widget_active_downloads", 0)
        val lastTitle = prefs.getString("widget_last_title", "لا توجد تحميلات") ?: "لا توجد تحميلات"

        views.setTextViewText(R.id.widget_active_count, "جاري: $activeCount")
        views.setTextViewText(R.id.widget_last_title, lastTitle)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        /** استدعِ هذا لتحديث الودجت من خارجه */
        fun updateWidgetData(context: Context, activeCount: Int, lastTitle: String) {
            context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt("widget_active_downloads", activeCount)
                .putString("widget_last_title", lastTitle)
                .apply()

            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            context.sendBroadcast(intent)
        }
    }
}
