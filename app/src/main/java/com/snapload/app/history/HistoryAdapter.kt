package com.snapload.app.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.snapload.app.R

/**
 * Adapter لعرض سجل البحث في HomeFragment
 */
class HistoryAdapter(
    private val onItemClick: (HistoryManager.HistoryItem) -> Unit,
    private val onDownloadClick: (HistoryManager.HistoryItem) -> Unit,
    private val onDeleteClick: (HistoryManager.HistoryItem) -> Unit
) : ListAdapter<HistoryManager.HistoryItem, HistoryAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnail: ImageView = itemView.findViewById(R.id.iv_history_thumbnail)
        val title: TextView = itemView.findViewById(R.id.tv_history_title)
        val platform: TextView = itemView.findViewById(R.id.tv_history_platform)
        val btnDownload: View = itemView.findViewById(R.id.btn_history_download)
        val btnDelete: View = itemView.findViewById(R.id.btn_history_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        holder.title.text = item.title
        holder.platform.text = item.platform

        Glide.with(holder.thumbnail)
            .load(item.thumbnail)
            .placeholder(R.drawable.ic_download_circle)
            .error(R.drawable.ic_download_circle)
            .centerCrop()
            .into(holder.thumbnail)

        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.btnDownload.setOnClickListener { onDownloadClick(item) }
        holder.btnDelete.setOnClickListener { onDeleteClick(item) }
    }

    private class DiffCallback : DiffUtil.ItemCallback<HistoryManager.HistoryItem>() {
        override fun areItemsTheSame(
            oldItem: HistoryManager.HistoryItem,
            newItem: HistoryManager.HistoryItem
        ) = oldItem.url == newItem.url

        override fun areContentsTheSame(
            oldItem: HistoryManager.HistoryItem,
            newItem: HistoryManager.HistoryItem
        ) = oldItem == newItem
    }
}
