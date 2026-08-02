package com.snapload.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.snapload.app.data.model.DownloadItem
import com.snapload.app.utils.Constants

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val downloadUrl: String,
    val thumbnail: String,
    val platform: String,
    val quality: String,
    val ext: String,
    val fileSize: Long = 0L,
    val filePath: String = "",
    val status: String = Constants.Status.PENDING,
    val progress: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val downloadManagerId: Long = -1L
) {
    fun toDownloadItem() = DownloadItem(
        id, title, url, downloadUrl, thumbnail,
        platform, quality, ext, fileSize, filePath,
        status, progress, createdAt, downloadManagerId
    )
}
