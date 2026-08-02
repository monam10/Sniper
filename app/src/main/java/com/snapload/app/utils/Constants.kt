package com.snapload.app.utils

import com.snapload.app.BuildConfig

object Constants {
    const val API_BASE_URL = BuildConfig.API_BASE_URL
    const val DB_NAME = "snapload_db"
    const val DOWNLOAD_CHANNEL_ID = "download_channel"
    const val DOWNLOAD_CHANNEL_NAME = "تحميلات SnapLoad"
    const val PREF_NAME = "snapload_prefs"
    const val PREF_DOWNLOAD_PATH = "download_path"
    const val PREF_DEFAULT_QUALITY = "default_quality"
    const val PREF_THEME = "theme"
    const val PREF_LANGUAGE = "language"
    const val PREF_WIFI_ONLY = "wifi_only"
    const val PREF_CONCURRENT_DOWNLOADS = "concurrent_downloads"
    const val MAX_CONCURRENT_DOWNLOADS = 3

    // Supported platforms
    val SUPPORTED_DOMAINS = listOf(
        "youtube.com", "youtu.be",
        "instagram.com",
        "tiktok.com", "vm.tiktok.com",
        "twitter.com", "x.com", "t.co",
        "facebook.com", "fb.watch",
        "dailymotion.com",
        "vimeo.com",
        "reddit.com",
        "pinterest.com",
        "soundcloud.com",
        "twitch.tv",
        "bilibili.com",
        "ok.ru",
        "vk.com"
    )

    object Status {
        const val PENDING = "pending"
        const val DOWNLOADING = "downloading"
        const val COMPLETED = "completed"
        const val FAILED = "failed"
        const val PAUSED = "paused"
    }
}
