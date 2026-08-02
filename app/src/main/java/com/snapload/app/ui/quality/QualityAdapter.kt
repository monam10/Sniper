package com.snapload.app.ui.quality

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.snapload.app.R
import com.snapload.app.data.model.VideoFormat
import com.snapload.app.databinding.ItemQualityBinding
import com.snapload.app.utils.gone
import com.snapload.app.utils.show

class QualityAdapter(
    private val onDownloadClick: (VideoFormat) -> Unit
) : ListAdapter<VideoFormat, QualityAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemQualityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(format: VideoFormat, isFirst: Boolean) {
            binding.apply {
                tvQuality.text = format.quality
                tvExt.text = format.ext.uppercase()
                tvSize.text = format.formattedSize().ifEmpty { "" }

                if (isFirst) {
                    badgeBest.show()
                } else {
                    badgeBest.gone()
                }

                when {
                    format.isAudioOnly() -> {
                        ivFormatIcon.setImageResource(R.drawable.ic_audio)
                    }
                    format.quality.contains("1080") || format.quality.contains("4K") -> {
                        ivFormatIcon.setImageResource(R.drawable.ic_hd)
                    }
                    else -> {
                        ivFormatIcon.setImageResource(R.drawable.ic_sd)
                    }
                }

                btnDownload.setOnClickListener {
                    btnDownload.isEnabled = false
                    onDownloadClick(format)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemQualityBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position == 0)
    }

    class DiffCallback : DiffUtil.ItemCallback<VideoFormat>() {
        override fun areItemsTheSame(oldItem: VideoFormat, newItem: VideoFormat) =
            oldItem.formatId == newItem.formatId
        override fun areContentsTheSame(oldItem: VideoFormat, newItem: VideoFormat) =
            oldItem == newItem
    }
}
