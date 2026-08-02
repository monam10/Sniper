package com.snapload.app.data.model

import com.google.gson.annotations.SerializedName

data class VideoInfo(
    @SerializedName("title") val title: String = "",
    @SerializedName("thumbnail") val thumbnail: String = "",
    @SerializedName("duration") val duration: Long? = null,
    @SerializedName("uploader") val uploader: String = "",
    @SerializedName("platform") val platform: String = "",
    @SerializedName("formats") val formats: List<VideoFormat> = emptyList(),
    @SerializedName("direct_url") val directUrl: String? = null,
    @SerializedName("error") val error: String? = null
)

data class VideoFormat(
    @SerializedName("format_id") val formatId: String = "",
    @SerializedName("quality") val quality: String = "",
    @SerializedName("ext") val ext: String = "",
    @SerializedName("type") val type: String = "",      // "video+audio" | "video" | "audio"
    @SerializedName("filesize") val filesize: Long? = null,
    @SerializedName("tbr") val tbr: Double? = null,
    @SerializedName("url") val url: String? = null
) {
    fun isVideoAndAudio() = type == "video+audio"
    fun isVideoOnly() = type == "video"
    fun isAudioOnly() = type == "audio"

    fun formattedSize(): String {
        if (filesize == null || filesize == 0L) return ""
        val mb = filesize / 1024.0 / 1024.0
        return if (mb >= 1000) "%.1f GB".format(mb / 1024) else "%.1f MB".format(mb)
    }
}

data class DownloadRequest(
    @SerializedName("url") val url: String,
    @SerializedName("format_id") val formatId: String = "best"
)

data class DownloadUrlResponse(
    @SerializedName("title") val title: String = "",
    @SerializedName("ext") val ext: String = "mp4",
    @SerializedName("direct_url") val directUrl: String? = null,
    @SerializedName("thumbnail") val thumbnail: String = "",
    @SerializedName("error") val error: String? = null
)
