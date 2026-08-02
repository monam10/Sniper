package com.snapload.app.ui.quality

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.snapload.app.R
import com.snapload.app.data.model.VideoFormat
import com.snapload.app.data.model.VideoInfo
import com.snapload.app.data.repository.DownloadRepository
import com.snapload.app.databinding.BottomSheetQualityBinding
import com.snapload.app.utils.gone
import com.snapload.app.utils.show
import com.snapload.app.utils.showToast

class QualityBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetQualityBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: QualityAdapter
    private lateinit var videoInfo: VideoInfo
    private val downloadRepository by lazy { DownloadRepository(requireContext()) }
    private var currentUrl: String = ""

    companion object {
        const val TAG = "QualityBottomSheet"
        private const val ARG_VIDEO_INFO = "video_info"
        private const val ARG_URL = "url"

        fun newInstance(info: VideoInfo, url: String = ""): QualityBottomSheet {
            return QualityBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_VIDEO_INFO, info as java.io.Serializable)
                    putString(ARG_URL, url)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // FIX: Theme name in XML is "Theme.SnapLoad.BottomSheetDialog"
        //      Android converts dots → underscores → R.style.Theme_SnapLoad_BottomSheetDialog
        setStyle(STYLE_NORMAL, R.style.Theme_SnapLoad_BottomSheetDialog)
        @Suppress("DEPRECATION")
        videoInfo = arguments?.getSerializable(ARG_VIDEO_INFO) as? VideoInfo ?: return
        currentUrl = arguments?.getString(ARG_URL) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetQualityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupHeader()
        setupTabs()
        setupAdapter()
        // FIX: XML uses id="ibQsClose", not "btnClose"
        binding.ibQsClose.setOnClickListener { dismiss() }
        binding.btnDownloadAll.setOnClickListener { downloadAll() }
    }

    private fun setupHeader() {
        // FIX: XML uses id="tvQsTitle", not "tvSheetTitle"
        binding.tvQsTitle.text = videoInfo.title
        // FIX: XML uses id="ivQsThumbnail", not "ivSheetThumbnail"
        Glide.with(requireContext())
            .load(videoInfo.thumbnail)
            .placeholder(R.drawable.ic_logo)
            .into(binding.ivQsThumbnail)
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.tab_video)))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.tab_audio)))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val isAudio = tab?.position == 1
                updateList(isAudio)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupAdapter() {
        adapter = QualityAdapter { format ->
            startDownload(format)
        }
        binding.rvQualities.adapter = adapter
        updateList(false)
    }

    private fun updateList(audioOnly: Boolean) {
        val formats = if (audioOnly) {
            videoInfo.formats.filter { it.isAudioOnly() }
                .sortedByDescending { it.tbr ?: 0.0 }
        } else {
            videoInfo.formats.filter { it.isVideoAndAudio() || it.isVideoOnly() }
                .sortedByDescending { it.tbr ?: 0.0 }
        }

        if (formats.isEmpty()) {
            // FIX: tvEmptyFormats not in XML — show toast and hide list instead
            binding.rvQualities.gone()
            requireContext().showToast(getString(R.string.no_formats_available))
        } else {
            binding.rvQualities.show()
            adapter.submitList(formats)
        }
    }

    private fun startDownload(format: VideoFormat) {
        downloadRepository.startDownload(
            url = currentUrl.ifEmpty { videoInfo.formats.firstOrNull()?.url ?: return },
            formatId = format.formatId,
            title = videoInfo.title,
            thumbnail = videoInfo.thumbnail,
            platform = videoInfo.platform,
            quality = format.quality,
            ext = format.ext
        )
        requireContext().showToast(getString(R.string.download_started))
        dismiss()
    }

    private fun downloadAll() {
        val videoFormats = videoInfo.formats
            .filter { it.isVideoAndAudio() }
            .sortedByDescending { it.tbr ?: 0.0 }
            .take(1)

        videoFormats.forEach { startDownload(it) }
        requireContext().showToast(getString(R.string.downloads_queued))
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
