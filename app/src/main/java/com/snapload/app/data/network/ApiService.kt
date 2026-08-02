package com.snapload.app.data.network

import com.snapload.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * ApiService المحدّث بإضافة endpoint /update و /formats
 * استبدل ApiService.kt بهذا الملف
 */
interface ApiService {
    @POST("info")
    suspend fun getVideoInfo(@Body request: Map<String, String>): Response<VideoInfo>

    @POST("download-url")
    suspend fun getDownloadUrl(@Body request: DownloadRequest): Response<DownloadUrlResponse>

    @POST("formats")
    suspend fun getFormats(@Body request: Map<String, String>): Response<FormatsResponse>

    @POST("update")
    suspend fun updateYtDlp(): Response<UpdateResponse>

    @GET("ping")
    suspend fun ping(): Response<Map<String, String>>
}

data class FormatsResponse(
    val formats: List<VideoFormat> = emptyList(),
    val title: String = ""
)

data class UpdateResponse(
    val status: String = "",
    val version: String = "",
    val error: String? = null
)
