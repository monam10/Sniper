package com.snapload.app.data.network

import android.content.Context
import com.snapload.app.BuildConfig
import com.snapload.app.network.cache.NetworkCacheInterceptor
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object ApiClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
    }

    @Volatile
    private var _apiService: ApiService? = null

    /**
     * Must be called from Application.onCreate() before any Repository is used.
     * Safe to call multiple times (idempotent).
     */
    fun init(context: Context) {
        if (_apiService != null) return
        synchronized(this) {
            if (_apiService != null) return

            val cacheDir = File(context.cacheDir, "http_cache")
            val cacheSize = 10L * 1024L * 1024L // 10 MB

            val okHttpClient = OkHttpClient.Builder()
                .addNetworkInterceptor(NetworkCacheInterceptor())
                .addInterceptor(loggingInterceptor)
                .cache(Cache(cacheDir, cacheSize))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL + "/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            _apiService = retrofit.create(ApiService::class.java)
        }
    }

    val apiService: ApiService
        get() = _apiService
            ?: error("ApiClient not initialized. Call ApiClient.init(context) from Application.onCreate().")
}
