package com.snapload.app.service

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.snapload.app.data.db.AppDatabase
import com.snapload.app.data.db.DownloadEntity
import com.snapload.app.data.network.NetworkResult
import com.snapload.app.data.repository.VideoRepository
import com.snapload.app.utils.Constants

class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_URL = "url"
        const val KEY_FORMAT_ID = "format_id"
        const val KEY_TITLE = "title"
        const val KEY_THUMBNAIL = "thumbnail"
        const val KEY_PLATFORM = "platform"
        const val KEY_QUALITY = "quality"
        const val KEY_EXT = "ext"
    }

    private val repository = VideoRepository()
    private val db = AppDatabase.getInstance(context)

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val formatId = inputData.getString(KEY_FORMAT_ID) ?: "best"
        val title = inputData.getString(KEY_TITLE) ?: "Unknown"
        val thumbnail = inputData.getString(KEY_THUMBNAIL) ?: ""
        val platform = inputData.getString(KEY_PLATFORM) ?: ""
        val quality = inputData.getString(KEY_QUALITY) ?: ""
        val ext = inputData.getString(KEY_EXT) ?: "mp4"

        // Insert into DB with PENDING status
        val entity = DownloadEntity(
            title = title,
            url = url,
            downloadUrl = "",
            thumbnail = thumbnail,
            platform = platform,
            quality = quality,
            ext = ext,
            status = Constants.Status.PENDING
        )
        val dbId = db.downloadDao().insert(entity)

        // Get direct download URL
        return when (val result = repository.getDownloadUrl(url, formatId)) {
            is NetworkResult.Success -> {
                val downloadUrl = result.data.directUrl ?: run {
                    db.downloadDao().updateProgress(dbId, Constants.Status.FAILED, 0)
                    return Result.failure()
                }

                // Update DB with actual download URL
                db.downloadDao().update(
                    entity.copy(id = dbId, downloadUrl = downloadUrl, status = Constants.Status.DOWNLOADING)
                )

                // Start DownloadService
                val serviceIntent = Intent(applicationContext, DownloadService::class.java).apply {
                    action = DownloadService.ACTION_START_DOWNLOAD
                    putExtra(DownloadService.EXTRA_DOWNLOAD_URL, downloadUrl)
                    putExtra(DownloadService.EXTRA_TITLE, title)
                    putExtra(DownloadService.EXTRA_THUMBNAIL, thumbnail)
                    putExtra(DownloadService.EXTRA_PLATFORM, platform)
                    putExtra(DownloadService.EXTRA_QUALITY, quality)
                    putExtra(DownloadService.EXTRA_EXT, ext)
                    putExtra(DownloadService.EXTRA_DB_ID, dbId)
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    applicationContext.startForegroundService(serviceIntent)
                } else {
                    applicationContext.startService(serviceIntent)
                }

                Result.success()
            }
            is NetworkResult.Error -> {
                db.downloadDao().updateProgress(dbId, Constants.Status.FAILED, 0)
                Result.failure()
            }
            is NetworkResult.Loading -> Result.retry()
        }
    }
}
