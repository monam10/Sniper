package com.snapload.app.network.cache

import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * يُخزّن استجابات /info لمدة 5 دقائق في cache
 * يمنع إعادة استدعاء yt-dlp لنفس الرابط
 */
class NetworkCacheInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        val shouldCache = request.url.encodedPath.endsWith("/info") ||
                          request.url.encodedPath.endsWith("/formats")

        return if (shouldCache) {
            val cacheControl = CacheControl.Builder()
                .maxAge(5, TimeUnit.MINUTES)
                .build()
            response.newBuilder()
                .removeHeader("Pragma")
                .removeHeader("Cache-Control")
                .header("Cache-Control", cacheControl.toString())
                .build()
        } else {
            response
        }
    }
}

class ForceCacheInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
        builder.cacheControl(CacheControl.FORCE_CACHE)
        return chain.proceed(builder.build())
    }
}
