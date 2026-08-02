package com.snapload.app.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.work.*
import com.snapload.app.data.db.AppDatabase
import com.snapload.app.data.model.DownloadItem
import com.snapload.app.service.DownloadWorker
import com.snapload.app.utils.Constants
import com.snapload.app.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DownloadRepository(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).downloadDao()

    fun getAllDownloads(): LiveData<List<DownloadItem>> =
        dao.getAllDownloads().map { list -> list.map { it.toDownloadItem() } }

    fun getDownloadsByStatus(status: String): LiveData<List<DownloadItem>> =
        dao.getDownloadsByStatus(status).map { list -> list.map { it.toDownloadItem() } }

    fun startDownload(
        url: String,
        formatId: String,
        title: String,
        thumbnail: String,
        platform: String,
        quality: String,
        ext: String
    ) {
        val inputData = workDataOf(
            DownloadWorker.KEY_URL to url,
            DownloadWorker.KEY_FORMAT_ID to formatId,
            DownloadWorker.KEY_TITLE to title,
            DownloadWorker.KEY_THUMBNAIL to thumbnail,
            DownloadWorker.KEY_PLATFORM to platform,
            DownloadWorker.KEY_QUALITY to quality,
            DownloadWorker.KEY_EXT to ext
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    suspend fun deleteDownload(item: DownloadItem) = withContext(Dispatchers.IO) {
        val entity = dao.getAllDownloads().value?.find { it.id == item.id }
        entity?.let {
            if (it.filePath.isNotEmpty()) {
                FileUtils.deleteFile(it.filePath)
            }
            dao.delete(it)
        }
    }

    suspend fun deleteById(id: Long, filePath: String = "") = withContext(Dispatchers.IO) {
        if (filePath.isNotEmpty()) FileUtils.deleteFile(filePath)
        dao.deleteById(id)
    }

    fun retryDownload(item: DownloadItem) {
        startDownload(
            url = item.url,
            formatId = "best",
            title = item.title,
            thumbnail = item.thumbnail,
            platform = item.platform,
            quality = item.quality,
            ext = item.ext
        )
    }

    suspend fun getActiveDownloadCount(): Int = dao.getActiveDownloadCount()
}
