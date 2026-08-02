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
import com.snapload.app.utils.*

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
        binding.btnAnalyze.setOnClickListener {
            val url = binding.etUrl.text.toString().trim()
            if (url.isEmpty()) {
                requireContext().showToast(getString(R.string.error_empty_url))
                return@setOnClickListener
            }
            viewModel.fetchVideoInfo(url)
        }

        binding.btnPasteAnalyze.setOnClickListener {
            val clipText = requireContext().getFromClipboard()
            if (clipText.isNullOrEmpty()) {
                requireContext().showToast(getString(R.string.clipboard_empty))
                return@setOnClickListener
            }
            binding.etUrl.setText(clipText)
            viewModel.fetchVideoInfo(clipText)
        }

        binding.btnPaste.setOnClickListener {
            val clipText = requireContext().getFromClipboard()
            if (!clipText.isNullOrEmpty()) {
                binding.etUrl.setText(clipText)
            }
        }

        binding.btnClear.setOnClickListener {
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

        binding.settingsIcon.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_settings)
        }
    }

    private fun observeViewModel() {
        viewModel.videoInfo.observe(viewLifecycleOwner) { result ->
            when (result) {
                null -> {
                    binding.loadingGroup.gone()
                    binding.cardVideoInfo.gone()
                }
                is NetworkResult.Loading -> {
                    binding.loadingGroup.show()
                    binding.cardVideoInfo.gone()
                    binding.tvLoadingText.text = getString(R.string.fetching_video_info)
                }
                is NetworkResult.Success -> {
                    binding.loadingGroup.gone()
                    displayVideoInfo(result.data)
                }
                is NetworkResult.Error -> {
                    binding.loadingGroup.gone()
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
            tvUploader.text = info.uploader
            tvPlatform.text = info.platform.replaceFirstChar { it.uppercaseChar() }
            tvDuration.text = (info.duration ?: 0L).toFormattedDuration()

            Glide.with(requireContext())
                .load(info.thumbnail)
                .placeholder(R.drawable.ic_logo)
                .into(ivThumbnail)

            val bestVideo = info.formats.filter { it.isVideoAndAudio() || it.isVideoOnly() }
                .maxByOrNull { it.tbr ?: 0.0 }
            if (bestVideo != null) {
                tvBestVideoQuality.text = "${bestVideo.quality} • ${bestVideo.ext.uppercase()}"
                tvBestVideoSize.text = bestVideo.formattedSize()
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
        val sheet = QualityBottomSheet.newInstance(info)
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
