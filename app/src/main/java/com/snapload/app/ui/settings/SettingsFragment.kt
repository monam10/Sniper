package com.snapload.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.snapload.app.R
import com.snapload.app.data.network.ApiClient
import com.snapload.app.databinding.FragmentSettingsBinding
import com.snapload.app.utils.Constants
import com.snapload.app.utils.showToast
import kotlinx.coroutines.launch
import java.io.File

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val prefs by lazy {
        requireContext().getSharedPreferences(Constants.PREF_NAME, android.content.Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSettings()
        setupListeners()
    }

    private fun loadSettings() {
        binding.apply {
            // Download settings
            // FIX: XML uses id="tvCurrentPath", not "tvDownloadPath"
            val savedPath = prefs.getString(
                Constants.PREF_DOWNLOAD_PATH,
                android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                ).absolutePath + "/SnapLoad"
            )
            tvCurrentPath.text = savedPath

            val savedQuality = prefs.getString(Constants.PREF_DEFAULT_QUALITY, "best")
            val qualityValues = arrayOf("best", "1080p", "720p", "480p", "360p")
            spinnerQuality.setSelection(qualityValues.indexOf(savedQuality).coerceAtLeast(0))

            switchWifiOnly.isChecked = prefs.getBoolean(Constants.PREF_WIFI_ONLY, false)

            // FIX: XML uses id="sliderConcurrent", not "sliderConcurrentDownloads"
            sliderConcurrent.value = prefs.getInt(Constants.PREF_CONCURRENT_DOWNLOADS, 2).toFloat()
            tvConcurrentValue.text = prefs.getInt(Constants.PREF_CONCURRENT_DOWNLOADS, 2).toString()

            // Theme settings
            val theme = prefs.getString(Constants.PREF_THEME, "dark")
            when (theme) {
                "dark" -> rgTheme.check(R.id.rbDark)
                "light" -> rgTheme.check(R.id.rbLight)
                else -> rgTheme.check(R.id.rbAuto)
            }

            // Language
            val lang = prefs.getString(Constants.PREF_LANGUAGE, "ar")
            spinnerLanguage.setSelection(if (lang == "ar") 0 else 1)

            // Server
            etApiUrl.setText(prefs.getString("api_url", Constants.API_BASE_URL))

            // App version
            tvAppVersion.text = try {
                requireContext().packageManager.getPackageInfo(
                    requireContext().packageName, 0
                ).versionName
            } catch (e: Exception) { "1.0.0" }
        }
    }

    private fun setupListeners() {
        binding.apply {
            // Download path — FIX: XML uses id="btnChangeFolder", not "btnChangeDownloadPath"
            btnChangeFolder.setOnClickListener {
                requireContext().showToast(getString(R.string.feature_coming_soon))
            }

            // Default quality
            spinnerQuality.onItemSelectedListener =
                object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: android.widget.AdapterView<*>?, view: View?, pos: Int, id: Long
                    ) {
                        val values = arrayOf("best", "1080p", "720p", "480p", "360p")
                        prefs.edit().putString(Constants.PREF_DEFAULT_QUALITY, values[pos]).apply()
                    }
                    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                }

            // WiFi only
            switchWifiOnly.setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(Constants.PREF_WIFI_ONLY, checked).apply()
            }

            // FIX: XML uses id="sliderConcurrent", not "sliderConcurrentDownloads"
            sliderConcurrent.addOnChangeListener { _, value, _ ->
                val intVal = value.toInt()
                tvConcurrentValue.text = intVal.toString()
                prefs.edit().putInt(Constants.PREF_CONCURRENT_DOWNLOADS, intVal).apply()
            }

            // Theme
            rgTheme.setOnCheckedChangeListener { _, checkedId ->
                val (theme, mode) = when (checkedId) {
                    R.id.rbDark  -> "dark"  to AppCompatDelegate.MODE_NIGHT_YES
                    R.id.rbLight -> "light" to AppCompatDelegate.MODE_NIGHT_NO
                    else         -> "auto"  to AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                prefs.edit().putString(Constants.PREF_THEME, theme).apply()
                AppCompatDelegate.setDefaultNightMode(mode)
            }

            // Language
            spinnerLanguage.onItemSelectedListener =
                object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: android.widget.AdapterView<*>?, view: View?, pos: Int, id: Long
                    ) {
                        val lang = if (pos == 0) "ar" else "en"
                        val current = prefs.getString(Constants.PREF_LANGUAGE, "ar")
                        if (lang != current) {
                            prefs.edit().putString(Constants.PREF_LANGUAGE, lang).apply()
                            requireActivity().recreate()
                        }
                    }
                    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                }

            // FIX: XML has no separate "btnSaveApiUrl" button — save on test, or use the
            //      test-connection button row.  The API URL EditText is directly saved here.
            // Save API URL when test connection is pressed (also persists changes)
            btnTestConnection.setOnClickListener {
                val url = etApiUrl.text.toString().trim()
                prefs.edit().putString("api_url", url).apply()
                requireContext().showToast(getString(R.string.settings_saved))

                tvServerStatus.text = "⏳ ${getString(R.string.testing_connection)}"
                lifecycleScope.launch {
                    try {
                        val response = ApiClient.apiService.ping()
                        if (response.isSuccessful) {
                            tvServerStatus.text = "🟢 ${getString(R.string.server_connected)}"
                        } else {
                            tvServerStatus.text = "🔴 ${getString(R.string.server_disconnected)}"
                        }
                    } catch (e: Exception) {
                        tvServerStatus.text = "🔴 ${getString(R.string.server_disconnected)}"
                    }
                }
            }

            // FIX: XML uses id="itemUpdateYtdlp" (parent container), not "btnUpdateYtdlp"
            itemUpdateYtdlp.setOnClickListener {
                lifecycleScope.launch {
                    try {
                        requireContext().showToast(getString(R.string.updating_ytdlp))
                    } catch (e: Exception) {
                        requireContext().showToast(
                            e.localizedMessage ?: getString(R.string.error_generic)
                        )
                    }
                }
            }

            // FIX: XML uses id="itemClearCache" (parent container), not "btnClearCache"
            itemClearCache.setOnClickListener {
                requireContext().cacheDir.deleteRecursively()
                requireContext().showToast(getString(R.string.cache_cleared))
            }

            // FIX: XML uses id="itemRateApp" (parent container), not "btnRateApp"
            itemRateApp.setOnClickListener {
                try {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            android.net.Uri.parse("market://details?id=${requireContext().packageName}")
                        )
                    )
                } catch (e: Exception) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            android.net.Uri.parse(
                                "https://play.google.com/store/apps/details?id=${requireContext().packageName}"
                            )
                        )
                    )
                }
            }

            // FIX: XML uses id="itemContactUs" (parent container), not "btnContact"
            itemContactUs.setOnClickListener {
                val email = Intent(Intent.ACTION_SENDTO).apply {
                    data = android.net.Uri.parse("mailto:support@snapload.app")
                    putExtra(Intent.EXTRA_SUBJECT, "SnapLoad Support")
                }
                startActivity(Intent.createChooser(email, getString(R.string.contact_us)))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
