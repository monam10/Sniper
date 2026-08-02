package com.snapload.app.utils

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

/**
 * تغيير لغة التطبيق وإعادة تشغيله
 */
object LanguageUtils {

    private const val PREF_LANG = "language"

    fun setLanguage(context: Context, langCode: String) {
        context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(PREF_LANG, langCode).apply()
        applyLanguage(context, langCode)
    }

    fun getLanguage(context: Context): String {
        return context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
            .getString(PREF_LANG, "ar") ?: "ar"
    }

    fun applyLanguage(context: Context, langCode: String) {
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        }
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    fun wrapContext(context: Context): Context {
        val langCode = getLanguage(context)
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }

    /** أعد تشغيل النشاط لتطبيق اللغة الجديدة */
    fun restartActivity(activity: Activity) {
        activity.finish()
        activity.startActivity(activity.intent)
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
