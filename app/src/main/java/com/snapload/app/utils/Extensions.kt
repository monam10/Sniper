package com.snapload.app.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

// ──────────────────── String Extensions ────────────────────

fun String.isValidUrl(): Boolean {
    return try {
        val url = java.net.URL(this)
        url.protocol in listOf("http", "https")
    } catch (e: Exception) {
        false
    }
}

fun String.extractUrls(): List<String> {
    val pattern = Regex("""https?://[^\s]+""")
    return pattern.findAll(this).map { it.value }.toList()
}

fun String.detectPlatform(): String {
    val host = try {
        java.net.URL(this).host.lowercase().removePrefix("www.")
    } catch (e: Exception) {
        return "Unknown"
    }
    return when {
        host.contains("youtube.com") || host.contains("youtu.be") -> "YouTube"
        host.contains("instagram.com") -> "Instagram"
        host.contains("tiktok.com") -> "TikTok"
        host.contains("twitter.com") || host.contains("x.com") || host.contains("t.co") -> "Twitter/X"
        host.contains("facebook.com") || host.contains("fb.watch") -> "Facebook"
        host.contains("dailymotion.com") -> "Dailymotion"
        host.contains("vimeo.com") -> "Vimeo"
        host.contains("reddit.com") -> "Reddit"
        host.contains("pinterest.com") -> "Pinterest"
        host.contains("soundcloud.com") -> "SoundCloud"
        host.contains("twitch.tv") -> "Twitch"
        host.contains("bilibili.com") -> "Bilibili"
        host.contains("ok.ru") -> "OK.ru"
        host.contains("vk.com") -> "VK"
        else -> "Unknown"
    }
}

fun String.toSafeFileName(): String {
    return this
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .replace(Regex("\\s+"), "_")
        .take(100)
}

// ──────────────────── Long Extensions ────────────────────

fun Long.toFormattedSize(): String {
    if (this <= 0L) return ""
    val kb = this / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "%.1f GB".format(gb)
        mb >= 1.0 -> "%.1f MB".format(mb)
        else -> "%.0f KB".format(kb)
    }
}

fun Long.toFormattedDuration(): String {
    if (this <= 0L) return ""
    val seconds = this % 60
    val minutes = (this / 60) % 60
    val hours = this / 3600
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

// ──────────────────── View Extensions ────────────────────

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.INVISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

// ──────────────────── Context Extensions ────────────────────

fun Context.copyToClipboard(label: String, text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

fun Context.getFromClipboard(): String? {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    return clipboard.primaryClip?.getItemAt(0)?.text?.toString()
}

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.isNetworkAvailable(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
           caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

// ──────────────────── Fragment Extensions ────────────────────

fun Fragment.showSnackbar(message: String, duration: Int = Snackbar.LENGTH_LONG) {
    view?.let { Snackbar.make(it, message, duration).show() }
}
