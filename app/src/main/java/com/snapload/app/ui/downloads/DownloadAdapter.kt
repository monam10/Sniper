package com.snapload.app.ui.downloads

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.snapload.app.R
import com.snapload.app.data.model.DownloadItem
import com.snapload.app.databinding.ItemDownloadBinding
import com.snapload.app.utils.Constants
import com.snapload.app.utils.gone
import com.snapload.app.utils.show
import com.snapload.app.utils.toFormattedSize

class DownloadAdapter(
    private val onOpenClick: (DownloadItem) -> Unit,
    private val onShareClick: (DownloadItem) -> Unit,
    private val onDeleteClick: (DownloadItem) -> Unit,
    private val onRetryClick: (DownloadItem) -> Unit
) : ListAdapter<DownloadItem, DownloadAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemDownloadBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DownloadItem) {
            binding.apply {
                tvDownloadTitle.text = item.title
                tvDownloadMeta.text = "${item.quality} • ${item.ext.uppercase()} • ${item.fileSize.toFormattedSize()}"

                Glide.with(itemView.context)
                    .load(item.thumbnail)
                    .placeholder(R.drawable.ic_logo)
                    .into(ivDownloadThumbnail)

                when (item.status) {
                    Constants.Status.DOWNLOADING -> {
                        progressDownload.show()
                        progressDownload.progress = item.progress
                        tvDownloadStatus.text = "⬇️ ${item.progress}%"
                        tvDownloadStatus.setTextColor(itemView.context.getColor(R.color.primary))
                    }
                    Constants.Status.COMPLETED -> {
                        progressDownload.gone()
                        tvDownloadStatus.text = "✅ ${itemView.context.getString(R.string.status_completed)}"
                        tvDownloadStatus.setTextColor(itemView.context.getColor(R.color.success))
                    }
                    Constants.Status.FAILED -> {
                        progressDownload.gone()
                        tvDownloadStatus.text = "❌ ${itemView.context.getString(R.string.status_failed)}"
                        tvDownloadStatus.setTextColor(itemView.context.getColor(R.color.error))
                    }
                    Constants.Status.PENDING -> {
                        progressDownload.show()
                        progressDownload.isIndeterminate = true
                        tvDownloadStatus.text = "⏳ ${itemView.context.getString(R.string.status_pending)}"
                        tvDownloadStatus.setTextColor(itemView.context.getColor(R.color.warning))
                    }
                    else -> {
                        progressDownload.gone()
                        tvDownloadStatus.text = item.status
                    }
                }

                btnOptions.setOnClickListener { showOptionsMenu(it, item) }

                root.setOnClickListener {
                    if (item.status == Constants.Status.COMPLETED) onOpenClick(item)
                }
            }
        }

        private fun showOptionsMenu(anchor: View, item: DownloadItem) {
            val popup = PopupMenu(anchor.context, anchor)
            popup.inflate(R.menu.download_item_menu)

            popup.menu.findItem(R.id.action_open)?.isVisible = item.status == Constants.Status.COMPLETED
            popup.menu.findItem(R.id.action_share)?.isVisible = item.status == Constants.Status.COMPLETED
            popup.menu.findItem(R.id.action_retry)?.isVisible = item.status == Constants.Status.FAILED

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_open -> onOpenClick(item)
                    R.id.action_share -> onShareClick(item)
                    R.id.action_delete -> onDeleteClick(item)
                    R.id.action_retry -> onRetryClick(item)
                }
                true
            }
            popup.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDownloadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<DownloadItem>() {
        override fun areItemsTheSame(oldItem: DownloadItem, newItem: DownloadItem) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: DownloadItem, newItem: DownloadItem) =
            oldItem == newItem
    }
}
