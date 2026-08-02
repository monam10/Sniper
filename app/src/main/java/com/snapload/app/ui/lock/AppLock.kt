package com.snapload.app.ui.lock

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.snapload.app.utils.Constants

/**
 * تشغيل/إيقاف قفل التطبيق من الإعدادات
 * دعم بصمة الإصبع (BiometricPrompt)
 * fallback لكلمة مرور رقمية (PIN 4 أرقام)
 */
object AppLock {

    private const val PREF_APP_LOCK_ENABLED = "app_lock_enabled"
    private const val PREF_PIN_HASH = "pin_hash"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)

    fun isLockEnabled(context: Context): Boolean =
        prefs(context).getBoolean(PREF_APP_LOCK_ENABLED, false)

    fun setLockEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(PREF_APP_LOCK_ENABLED, enabled).apply()
    }

    fun setPin(context: Context, pin: String) {
        // تخزين hash بسيط (في تطبيق حقيقي استخدم bcrypt أو SHA-256 مع salt)
        val hash = pin.hashCode().toString()
        prefs(context).edit().putString(PREF_PIN_HASH, hash).apply()
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val storedHash = prefs(context).getString(PREF_PIN_HASH, null) ?: return false
        return storedHash == pin.hashCode().toString()
    }

    fun isBiometricAvailable(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFallback: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED
                ) {
                    onFallback()
                } else {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                onError("فشل التحقق، حاول مجدداً")
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("قفل SnapLoad")
            .setSubtitle("استخدم بصمة الإصبع لفتح التطبيق")
            .setNegativeButtonText("استخدم الرقم السري")
            .build()

        prompt.authenticate(info)
    }
}
