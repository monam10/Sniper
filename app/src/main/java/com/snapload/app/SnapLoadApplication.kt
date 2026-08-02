package com.snapload.app

import android.app.Application
import com.snapload.app.data.network.ApiClient
import com.snapload.app.utils.LanguageUtils
import com.snapload.app.utils.ThemeManager

class SnapLoadApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Apply saved theme (must be before any Activity starts)
        ThemeManager.applyTheme(this)

        // 2. Apply saved language
        LanguageUtils.applyLanguage(this, LanguageUtils.getLanguage(this))

        // 3. Init ApiClient (must be done before any Repository call)
        ApiClient.init(this)
    }
}
