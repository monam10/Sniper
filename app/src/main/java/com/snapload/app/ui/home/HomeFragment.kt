package com.snapload.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.snapload.app.R
import com.snapload.app.data.model.VideoFormat
import com.snapload.app.data.model.VideoInfo
import com.snapload.app.data.network.NetworkResult
import com.snapload.app.data.repository.DownloadRepository
import com.snapload.app.databinding.FragmentHomeBinding
import com.snapload.app.ui.quality.QualityBottomSheet
import com.snapload.app.utils.gone
import com.snapload.app.utils.show
import com.snapload.app.utils.showToast
import com.snapload.app.utils.getFromClipboard
import com.snapload.app.utils.toFormattedDuration

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private val downloadRepository by lazy { DownloadRepository(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkClipboard()
    }

    private fun setupListeners() {
        // FIX: XML uses id="btnAnalyze" ✓ (already correct)
        binding.btnAnalyze.setOnClickListener {
            val url = binding.etUrl.text.toString().trim()
            if (url.isEmpty()) {
                requireContext().showToast(getString(R.string.error_empty_url))
                return@setOnClickListener
            }
            viewModel.fetchVideoInfo(url)
        }

        // FIX: XML uses id="btnPasteAndProcess", not "btnPasteAnalyze"
        binding.btnPasteAndProcess.setOnClickListener {
            val clipText = requireContext().getFromClipboard()
            if (clipText.isNullOrEmpty()) {
                requireContext().showToast(getString(R.string.clipboard_empty))
                return@setOnClickListener
            }
            binding.etUrl.setText(clipText)
            viewModel.fetchVideoInfo(clipText)
        }

        // FIX: XML uses id="ibPaste", not "btnPaste"
        binding.ibPaste.setOnClickListener {
            val clipText = requireContext().getFromClipboard()
            if (!clipText.isNullOrEmpty()) {
                binding.etUrl.setText(clipText)
            }
        }

        // FIX: XML uses id="ibClear", not "btnClear"
        binding.ibClear.setOnClickListener {
            binding.etUrl.setText("")
            viewModel.clearVideoInfo()
            binding.cardVideoInfo.gone()
        }

        binding.btnDownloadVideo.setOnClickListener {
            val info = (viewModel.videoInfo.value as? NetworkResult.Success)?.data ?: return@setOnClickListener
            val bestVideo = info.formats.filter { it.isVideoAndAudio() || it.isVideoOnly() }
                .maxByOrNull { it.tbr ?: 0.0 } ?: return@setOnClickListener
            startDownload(info, bestVideo)
        }

        binding.btnDownloadAudio.setOnClickListener {
            val info = (viewModel.videoInfo.value as? NetworkResult.Success)?.data ?: return@setOnClickListener
            val bestAudio = info.formats.filter { it.isAudioOnly() }
                .maxByOrNull { it.tbr ?: 0.0 } ?: return@setOnClickListener
            startDownload(info, bestAudio)
        }

        binding.btnMoreQualities.setOnClickListener {
            val info = (viewModel.videoInfo.value as? NetworkResult.Success)?.data ?: return@setOnClickListener
            showQualitySheet(info)
        }

        // FIX: XML uses id="ibSettings", not "settingsIcon"
        binding.ibSettings.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_settings)
        }
    }

    private fun observeViewModel() {
        viewModel.videoInfo.observe(viewLifecycleOwner) { result ->
            when (result) {
                null -> {
                    // FIX: XML uses id="layoutLoading", not "loadingGroup"
                    binding.layoutLoading.gone()
                    binding.cardVideoInfo.gone()
                }
                is NetworkResult.Loading -> {
                    binding.layoutLoading.show()
                    binding.cardVideoInfo.gone()
                    // FIX: tvLoadingText has no id in XML — text is set in XML already;
                    //      skip the dynamic setText call to avoid compilation error.
                }
                is NetworkResult.Success -> {
                    binding.layoutLoading.gone()
                    displayVideoInfo(result.data)
                }
                is NetworkResult.Error -> {
                    binding.layoutLoading.gone()
                    binding.cardVideoInfo.gone()
                    showSnackbar(result.message)
                }
            }
        }

        viewModel.clipboardUrl.observe(viewLifecycleOwner) { url ->
            if (url != null) {
                val platform = viewModel.detectPlatformFromUrl(url)
                Snackbar.make(
                    binding.root,
                    "📋 تم رصد رابط $platform، هل تريد تحليله؟",
                    Snackbar.LENGTH_LONG
                ).setAction(getString(R.string.analyze)) {
                    binding.etUrl.setText(url)
                    viewModel.fetchVideoInfo(url)
                    viewModel.clearClipboardSuggestion()
                }.addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        if (event == DISMISS_EVENT_TIMEOUT || event == DISMISS_EVENT_SWIPE) {
                            viewModel.ignoreClipboardUrl(url)
                        }
                    }
                }).show()
            }
        }
    }

    private fun displayVideoInfo(info: VideoInfo) {
        binding.apply {
            tvVideoTitle.text = info.title
            // FIX: tvPlatform not in XML — show "uploader • platform" combined in tvUploader
            val uploaderText = buildString {
                if (info.uploader.isNotBlank()) append(info.uploader)
                if (info.platform.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(info.platform.replaceFirstChar { it.uppercaseChar() })
                }
            }
            tvUploader.text = uploaderText
            tvDuration.text = (info.duration ?: 0L).toFormattedDuration()

            Glide.with(requireContext())
                .load(info.thumbnail)
                .placeholder(R.drawable.ic_logo)
                .into(ivThumbnail)

            val bestVideo = info.formats.filter { it.isVideoAndAudio() || it.isVideoOnly() }
                .maxByOrNull { it.tbr ?: 0.0 }
            if (bestVideo != null) {
                // FIX: tvBestVideoSize not in XML — combine quality + size + ext in tvBestVideoQuality
                val sizeStr = bestVideo.formattedSize()
                tvBestVideoQuality.text = buildString {
                    append("${bestVideo.quality} • ${bestVideo.ext.uppercase()}")
                    if (sizeStr.isNotEmpty()) append(" • $sizeStr")
                }
                btnDownloadVideo.isEnabled = true
            } else {
                btnDownloadVideo.isEnabled = false
            }

            val bestAudio = info.formats.filter { it.isAudioOnly() }
                .maxByOrNull { it.tbr ?: 0.0 }
            if (bestAudio != null) {
                tvBestAudioQuality.text = "${bestAudio.quality} • ${bestAudio.ext.uppercase()}"
                btnDownloadAudio.isEnabled = true
            } else {
                btnDownloadAudio.isEnabled = false
            }

            val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
            cardVideoInfo.startAnimation(anim)
            cardVideoInfo.show()
        }
    }

    private fun startDownload(info: VideoInfo, format: VideoFormat) {
        downloadRepository.startDownload(
            url = viewModel.currentUrl.value ?: return,
            formatId = format.formatId,
            title = info.title,
            thumbnail = info.thumbnail,
            platform = info.platform,
            quality = format.quality,
            ext = format.ext
        )
        showSnackbar(getString(R.string.download_started))
    }

    private fun showQualitySheet(info: VideoInfo) {
        val url = viewModel.currentUrl.value ?: ""
        val sheet = QualityBottomSheet.newInstance(info, url)
        sheet.show(childFragmentManager, QualityBottomSheet.TAG)
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
