package com.snapload.app.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.snapload.app.R
import com.snapload.app.data.db.AppDatabase
import com.snapload.app.data.db.DownloadEntity
import com.snapload.app.utils.Constants
import com.snapload.app.utils.toSafeFileName
import kotlinx.coroutines.*
import java.io.File

class DownloadService : Service() {

    companion object {
        const val ACTION_START_DOWNLOAD = "com.snapload.app.START_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.snapload.app.CANCEL_DOWNLOAD"
        const val EXTRA_DOWNLOAD_URL = "download_url"
        const val EXTRA_TITLE = "title"
        const val EXTRA_THUMBNAIL = "thumbnail"
        const val EXTRA_PLATFORM = "platform"
        const val EXTRA_QUALITY = "quality"
        const val EXTRA_EXT = "ext"
        const val EXTRA_ORIGINAL_URL = "original_url"
        const val EXTRA_FORMAT_ID = "format_id"
        const val EXTRA_DB_ID = "db_id"
        const val EXTRA_DM_ID = "dm_id"
        private const val NOTIFICATION_ID_BASE = 2000
        private const val CHANNEL_ID = Constants.DOWNLOAD_CHANNEL_ID
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val downloadManager by lazy { getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager }
    private val db by lazy { AppDatabase.getInstance(this) }
    private val activeDownloads = mutableMapOf<Long, Long>() // dmId -> dbId
    private val handler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val dmId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (dmId != -1L && activeDownloads.containsKey(dmId)) {
                onDownloadComplete(dmId)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        startProgressMonitor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> handleStartDownload(intent)
            ACTION_CANCEL_DOWNLOAD -> {
                val dmId = intent.getLongExtra(EXTRA_DM_ID, -1L)
                cancelDownload(dmId)
            }
        }
        return START_STICKY
    }

    private fun handleStartDownload(intent: Intent) {
        val downloadUrl = intent.getStringExtra(EXTRA_DOWNLOAD_URL) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "SnapLoad"
        val ext = intent.getStringExtra(EXTRA_EXT) ?: "mp4"
        val dbId = intent.getLongExtra(EXTRA_DB_ID, -1L)

        val fileName = "${title.toSafeFileName()}_${System.currentTimeMillis()}.$ext"
        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle(title)
            setDescription(getString(R.string.downloading))
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
            setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, "SnapLoad/$fileName")
            addRequestHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/91.0 Mobile Safari/537.36")
        }

        val dmId = downloadManager.enqueue(request)
        activeDownloads[dmId] = dbId

        startForeground(NOTIFICATION_ID_BASE + dbId.toInt(), buildProgressNotification(title, 0, dmId))

        serviceScope.launch {
            db.downloadDao().updateProgress(dbId, Constants.Status.DOWNLOADING, 0)
        }
    }

    private fun startProgressMonitor() {
        progressRunnable = object : Runnable {
            override fun run() {
                updateAllProgress()
                handler.postDelayed(this, 1500)
            }
        }
        handler.post(progressRunnable!!)
    }

    private fun updateAllProgress() {
        if (activeDownloads.isEmpty()) return
        activeDownloads.forEach { (dmId, dbId) ->
            val query = DownloadManager.Query().setFilterById(dmId)
            val cursor: Cursor? = downloadManager.query(query)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val bytesDownloaded = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val bytesTotal = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val progress = if (bytesTotal > 0) ((bytesDownloaded * 100) / bytesTotal).toInt() else 0
                    val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val title = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)) ?: "SnapLoad"

                    if (status == DownloadManager.STATUS_RUNNING) {
                        updateNotification(dbId, title, progress, dmId)
                        serviceScope.launch {
                            db.downloadDao().updateProgress(dbId, Constants.Status.DOWNLOADING, progress)
                        }
                    }
                }
            }
        }
    }

    private fun onDownloadComplete(dmId: Long) {
        val dbId = activeDownloads[dmId] ?: return
        val query = DownloadManager.Query().setFilterById(dmId)
        val cursor = downloadManager.query(query)
        cursor?.use { c ->
            if (c.moveToFirst()) {
                val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val localUri = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)) ?: ""
                val title = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)) ?: "SnapLoad"

                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    serviceScope.launch {
                        db.downloadDao().updateByDownloadManagerId(dmId, Constants.Status.COMPLETED, localUri)
                    }
                    showCompletedNotification(dbId.toInt(), title, localUri)
                } else if (status == DownloadManager.STATUS_FAILED) {
                    serviceScope.launch {
                        db.downloadDao().updateProgress(dbId, Constants.Status.FAILED, 0)
                    }
                    showFailedNotification(dbId.toInt(), title)
                }
            }
        }
        activeDownloads.remove(dmId)
        if (activeDownloads.isEmpty()) stopForeground(true)
    }

    private fun cancelDownload(dmId: Long) {
        if (dmId == -1L) return
        val dbId = activeDownloads[dmId]
        downloadManager.remove(dmId)
        activeDownloads.remove(dmId)
        dbId?.let {
            serviceScope.launch {
                db.downloadDao().updateProgress(it, Constants.Status.FAILED, 0)
            }
        }
        if (activeDownloads.isEmpty()) stopForeground(true)
    }

    private fun buildProgressNotification(title: String, progress: Int, dmId: Long): Notification {
        val cancelIntent = Intent(this, DownloadService::class.java).apply {
            action = ACTION_CANCEL_DOWNLOAD
            putExtra(EXTRA_DM_ID, dmId)
        }
        val cancelPi = PendingIntent.getService(this, dmId.toInt(), cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download_circle)
            .setContentTitle(title)
            .setContentText("$progress%")
            .setProgress(100, progress, progress == 0)
            .addAction(R.drawable.ic_clear, getString(R.string.cancel), cancelPi)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(dbId: Long, title: String, progress: Int, dmId: Long) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID_BASE + dbId.toInt(), buildProgressNotification(title, progress, dmId))
    }

    private fun showCompletedNotification(id: Int, title: String, filePath: String) {
        val openIntent = PendingIntent.getActivity(this, id,
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(filePath), "video/*")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download_circle)
            .setContentTitle(getString(R.string.download_complete))
            .setContentText(title)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID_BASE + id, n)
    }

    private fun showFailedNotification(id: Int, title: String) {
        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download_circle)
            .setContentTitle(getString(R.string.download_failed))
            .setContentText(title)
            .setAutoCancel(true)
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID_BASE + id, n)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                Constants.DOWNLOAD_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "تحميلات SnapLoad"
                setShowBadge(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        progressRunnable?.let { handler.removeCallbacks(it) }
        serviceScope.cancel()
        try { unregisterReceiver(downloadReceiver) } catch (_: Exception) {}
    }
}
