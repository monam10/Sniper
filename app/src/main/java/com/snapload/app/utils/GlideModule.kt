package com.snapload.app.utils

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions

/**
 * إعداد Glide: cache + placeholder + حجم ذاكرة مناسب
 */
@GlideModule
class SnapLoadGlideModule : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // ذاكرة 20 MB للـ cache
        val memoryCacheSizeBytes = 1024 * 1024 * 20L
        builder.setMemoryCache(LruResourceCache(memoryCacheSizeBytes))

        // 100 MB قرص للـ cache
        val diskCacheSizeBytes = 1024 * 1024 * 100
        builder.setDiskCache(
            InternalCacheDiskCacheFactory(context, diskCacheSizeBytes)
        )

        // تنسيق صور لتوفير الذاكرة
        builder.setDefaultRequestOptions(
            RequestOptions()
                .format(DecodeFormat.PREFER_RGB_565)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
        )
    }

    // استخدم manifest parsing بشكل يدوي
    override fun isManifestParsingEnabled(): Boolean = false
}
