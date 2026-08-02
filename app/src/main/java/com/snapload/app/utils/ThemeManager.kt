package com.snapload.app.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

/**
 * يدير الثيم (داكن / فاتح / تلقائي) ويحفظ الاختيار
 */
object ThemeManager {

    private const val THEME_DARK = "dark"
    private const val THEME_LIGHT = "light"
    private const val THEME_AUTO = "auto"

    fun applyTheme(context: Context) {
        val prefs: SharedPreferences =
            context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        when (prefs.getString(Constants.PREF_THEME, THEME_DARK)) {
            THEME_DARK  -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else        -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    fun setTheme(context: Context, theme: String) {
        context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(Constants.PREF_THEME, theme).apply()
        when (theme) {
            THEME_DARK  -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else        -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    fun getTheme(context: Context): String {
        return context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
            .getString(Constants.PREF_THEME, THEME_DARK) ?: THEME_DARK
    }

    fun isDarkMode(context: Context): Boolean = getTheme(context) == THEME_DARK
}
