package com.snapload.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.snapload.app.MainActivity
import com.snapload.app.R
import com.snapload.app.utils.Constants

object NotificationHelper {

    private const val CHANNEL_ID = Constants.DOWNLOAD_CHANNEL_ID
    private const val CHANNEL_NAME = Constants.DOWNLOAD_CHANNEL_NAME
    private const val CHANNEL_DESCRIPTION = "إشعارات تحميلات SnapLoad"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun buildDownloadingNotification(
        context: Context,
        notificationId: Int,
        title: String,
        progress: Int,
        cancelIntent: PendingIntent? = null
    ): NotificationCompat.Builder {
        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download_circle)
            .setContentTitle(title)
            .setContentText("جاري التحميل... $progress%")
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .apply {
                if (cancelIntent != null) {
                    addAction(
                        android.R.drawable.ic_delete,
                        "إلغاء",
                        cancelIntent
                    )
                }
            }
    }

    fun showDownloadingNotification(
        context: Context,
        notificationId: Int,
        title: String,
        progress: Int,
        cancelIntent: PendingIntent? = null
    ) {
        val notification = buildDownloadingNotification(context, notificationId, title, progress, cancelIntent).build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted
        }
    }

    fun showDownloadCompleteNotification(
        context: Context,
        notificationId: Int,
        title: String,
        filePath: String,
        mimeType: String = "video/*"
    ) {
        val openFileIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(android.net.Uri.parse(filePath), mimeType)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val openPending = PendingIntent.getActivity(
            context, notificationId + 1000,
            openFileIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(filePath))
        }
        val sharePending = PendingIntent.getActivity(
            context, notificationId + 2000,
            Intent.createChooser(shareIntent, "مشاركة"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("اكتمل التحميل ✅")
            .setContentText(title)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_view, "فتح", openPending)
            .addAction(android.R.drawable.ic_menu_share, "مشاركة", sharePending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted
        }
    }

    fun showDownloadFailedNotification(
        context: Context,
        notificationId: Int,
        title: String,
        retryIntent: PendingIntent? = null
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("فشل التحميل ❌")
            .setContentText(title)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .apply {
                if (retryIntent != null) {
                    addAction(android.R.drawable.ic_menu_rotate, "إعادة المحاولة", retryIntent)
                }
            }
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted
        }
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    fun cancelAllNotifications(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
