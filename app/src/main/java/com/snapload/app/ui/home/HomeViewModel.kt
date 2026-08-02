package com.snapload.app.ui.home

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.snapload.app.data.model.VideoInfo
import com.snapload.app.data.network.NetworkResult
import com.snapload.app.data.repository.VideoRepository
import com.snapload.app.utils.Constants
import com.snapload.app.utils.isValidUrl
import com.snapload.app.utils.detectPlatform
import kotlinx.coroutines.launch
import java.net.URL

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VideoRepository()

    private val _videoInfo = MutableLiveData<NetworkResult<VideoInfo>>()
    val videoInfo: LiveData<NetworkResult<VideoInfo>> = _videoInfo

    private val _clipboardUrl = MutableLiveData<String?>()
    val clipboardUrl: LiveData<String?> = _clipboardUrl

    private val _currentUrl = MutableLiveData<String>("")
    val currentUrl: LiveData<String> = _currentUrl

    private var lastIgnoredUrl: String? = null

    fun fetchVideoInfo(url: String) {
        if (!url.isValidUrl()) {
            _videoInfo.value = NetworkResult.Error("الرابط غير صالح. تأكد من نسخ رابط صحيح.")
            return
        }
        if (!isPlatformSupported(url)) {
            _videoInfo.value = NetworkResult.Error("هذه المنصة غير مدعومة حالياً.")
            return
        }
        _currentUrl.value = url
        _videoInfo.value = NetworkResult.Loading
        viewModelScope.launch {
            _videoInfo.value = repository.getVideoInfo(url)
        }
    }

    fun isPlatformSupported(url: String): Boolean {
        return try {
            val host = URL(url).host.lowercase().removePrefix("www.")
            Constants.SUPPORTED_DOMAINS.any { domain -> host.contains(domain) }
        } catch (e: Exception) {
            false
        }
    }

    fun detectPlatformFromUrl(url: String): String = url.detectPlatform()

    fun checkClipboard() {
        val context = getApplication<Application>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: return
        if (text.isValidUrl() && isPlatformSupported(text) && text != lastIgnoredUrl) {
            _clipboardUrl.value = text
        }
    }

    fun ignoreClipboardUrl(url: String) {
        lastIgnoredUrl = url
        _clipboardUrl.value = null
    }

    fun clearClipboardSuggestion() {
        _clipboardUrl.value = null
    }

    fun clearVideoInfo() {
        _videoInfo.value = null
    }
}
