package com.snapload.app.ui.share

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.snapload.app.R
import com.snapload.app.data.network.NetworkResult
import com.snapload.app.data.repository.VideoRepository
import com.snapload.app.databinding.ActivityShareHandlerBinding
import com.snapload.app.ui.quality.QualityBottomSheet
import com.snapload.app.utils.gone
import com.snapload.app.utils.isValidUrl
import com.snapload.app.utils.show
import com.snapload.app.utils.showToast
import kotlinx.coroutines.launch

class ShareHandlerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShareHandlerBinding
    private val repository = VideoRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShareHandlerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedUrl = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim() ?: run {
                showToast(getString(R.string.error_no_url))
                finish()
                return
            }

            if (!sharedUrl.isValidUrl()) {
                showToast(getString(R.string.error_invalid_url))
                finish()
                return
            }

            fetchVideoInfo(sharedUrl)
        } else {
            finish()
        }
    }

    private fun fetchVideoInfo(url: String) {
        binding.tvShareUrl.text = url
        binding.progressShare.show()
        binding.tvShareStatus.text = getString(R.string.fetching_video_info)

        lifecycleScope.launch {
            when (val result = repository.getVideoInfo(url)) {
                is NetworkResult.Success -> {
                    binding.progressShare.gone()
                    val sheet = QualityBottomSheet.newInstance(result.data, url)
                    sheet.show(supportFragmentManager, QualityBottomSheet.TAG)
                    supportFragmentManager.setFragmentResultListener(
                        "sheet_dismissed", this@ShareHandlerActivity
                    ) { _, _ -> finish() }
                }
                is NetworkResult.Error -> {
                    binding.progressShare.gone()
                    binding.tvShareStatus.text = result.message
                    showToast(result.message)
                    finish()
                }
                is NetworkResult.Loading -> { /* handled above */ }
            }
        }
    }
}
