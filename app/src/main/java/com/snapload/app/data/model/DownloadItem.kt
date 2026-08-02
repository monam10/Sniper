package com.snapload.app.data.model

data class DownloadItem(
    val id: Long = 0,
    val title: String,
    val url: String,
    val downloadUrl: String,
    val thumbnail: String,
    val platform: String,
    val quality: String,
    val ext: String,
    val fileSize: Long,
    val filePath: String,
    val status: String,
    val progress: Int,
    val createdAt: Long,
    val downloadManagerId: Long = -1L
)
