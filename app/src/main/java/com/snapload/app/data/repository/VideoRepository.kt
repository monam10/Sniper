package com.snapload.app.data.repository

import com.snapload.app.data.model.*
import com.snapload.app.data.network.ApiClient
import com.snapload.app.data.network.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoRepository {
    private val api = ApiClient.apiService

    suspend fun getVideoInfo(url: String): NetworkResult<VideoInfo> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getVideoInfo(mapOf("url" to url))
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.error != null) {
                        NetworkResult.Error(body.error)
                    } else if (body != null) {
                        NetworkResult.Success(body)
                    } else {
                        NetworkResult.Error("استجابة فارغة من السيرفر")
                    }
                } else {
                    NetworkResult.Error("خطأ ${response.code()}: ${response.message()}", response.code())
                }
            } catch (e: Exception) {
                NetworkResult.Error(e.localizedMessage ?: "خطأ في الاتصال بالإنترنت")
            }
        }

    suspend fun getDownloadUrl(url: String, formatId: String): NetworkResult<DownloadUrlResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getDownloadUrl(DownloadRequest(url, formatId))
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.error != null) {
                        NetworkResult.Error(body.error)
                    } else if (body != null && body.directUrl != null) {
                        NetworkResult.Success(body)
                    } else {
                        NetworkResult.Error("لا يوجد رابط تحميل متاح")
                    }
                } else {
                    NetworkResult.Error("خطأ ${response.code()}", response.code())
                }
            } catch (e: Exception) {
                NetworkResult.Error(e.localizedMessage ?: "خطأ في الاتصال")
            }
        }
}
