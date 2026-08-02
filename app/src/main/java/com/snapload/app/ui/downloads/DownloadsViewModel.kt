package com.snapload.app.ui.downloads

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.snapload.app.data.model.DownloadItem
import com.snapload.app.data.repository.DownloadRepository
import com.snapload.app.utils.Constants
import kotlinx.coroutines.launch

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DownloadRepository(application)

    private val _selectedTab = MutableLiveData(TAB_ALL)
    val selectedTab: LiveData<Int> = _selectedTab

    val downloads: LiveData<List<DownloadItem>> = _selectedTab.switchMap { tab ->
        when (tab) {
            TAB_DOWNLOADING -> repository.getDownloadsByStatus(Constants.Status.DOWNLOADING)
            TAB_COMPLETED -> repository.getDownloadsByStatus(Constants.Status.COMPLETED)
            else -> repository.getAllDownloads()
        }
    }

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun deleteDownload(item: DownloadItem) {
        viewModelScope.launch {
            repository.deleteById(item.id, item.filePath)
        }
    }

    fun retryDownload(item: DownloadItem) {
        repository.retryDownload(item)
    }

    companion object {
        const val TAB_DOWNLOADING = 0
        const val TAB_COMPLETED = 1
        const val TAB_ALL = 2
    }
}
