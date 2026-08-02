package com.snapload.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.snapload.app.data.db.AppDatabase
import com.snapload.app.data.network.ApiClient
import com.snapload.app.databinding.ActivitySplashBinding
import com.snapload.app.utils.PermissionHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Start logo animation
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        binding.ivLogo.startAnimation(fadeIn)
        binding.tvAppName.startAnimation(fadeIn)

        lifecycleScope.launch {
            // 1. Init Room DB in background
            AppDatabase.getInstance(applicationContext)

            // 2. Ping server
            try {
                ApiClient.apiService.ping()
            } catch (_: Exception) { /* silent - offline mode is fine */ }

            // 3. Request permissions if needed
            if (!PermissionHelper.checkStoragePermission(this@SplashActivity)) {
                PermissionHelper.requestStoragePermission(this@SplashActivity)
            }
            if (!PermissionHelper.checkNotificationPermission(this@SplashActivity)) {
                PermissionHelper.requestNotificationPermission(this@SplashActivity)
            }

            // 4. Wait minimum 2 seconds for splash
            delay(2000L)

            // 5. Navigate to MainActivity
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
}
