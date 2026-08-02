package com.snapload.app.ui.downloads

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.snapload.app.R
import com.snapload.app.data.model.DownloadItem
import com.snapload.app.databinding.FragmentDownloadsBinding
import com.snapload.app.utils.FileUtils
import com.snapload.app.utils.gone
import com.snapload.app.utils.show
import com.snapload.app.utils.showToast

class DownloadsFragment : Fragment() {

    private var _binding: FragmentDownloadsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DownloadsViewModel by viewModels()
    private lateinit var adapter: DownloadAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTabs()
        setupRecyclerView()
        setupSwipeToDelete()
        observeViewModel()
    }

    private fun setupTabs() {
        binding.tabLayoutDownloads.apply {
            addTab(newTab().setText(getString(R.string.tab_downloading)))
            addTab(newTab().setText(getString(R.string.tab_completed)))
            addTab(newTab().setText(getString(R.string.tab_all)))
            getTabAt(DownloadsViewModel.TAB_ALL)?.select()
        }

        binding.tabLayoutDownloads.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                viewModel.selectTab(tab?.position ?: DownloadsViewModel.TAB_ALL)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        adapter = DownloadAdapter(
            onOpenClick = { item -> openFile(item) },
            onShareClick = { item -> shareFile(item) },
            onDeleteClick = { item -> confirmDelete(item) },
            onRetryClick = { item -> retryDownload(item) }
        )
        binding.rvDownloads.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDownloads.adapter = adapter
    }

    private fun setupSwipeToDelete() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = adapter.currentList[position]
                viewModel.deleteDownload(item)
                Snackbar.make(binding.root, getString(R.string.download_deleted), Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.undo)) {
                        viewModel.retryDownload(item)
                    }.show()
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvDownloads)
    }

    private fun observeViewModel() {
        viewModel.downloads.observe(viewLifecycleOwner) { downloads ->
            adapter.submitList(downloads)
            if (downloads.isEmpty()) {
                binding.emptyState.show()
                binding.rvDownloads.gone()
            } else {
                binding.emptyState.gone()
                binding.rvDownloads.show()
            }
        }
    }

    private fun openFile(item: DownloadItem) {
        if (item.filePath.isEmpty()) {
            requireContext().showToast(getString(R.string.file_not_found))
            return
        }
        FileUtils.openFile(requireContext(), item.filePath, item.ext)
    }

    private fun shareFile(item: DownloadItem) {
        if (item.filePath.isEmpty()) {
            requireContext().showToast(getString(R.string.file_not_found))
            return
        }
        FileUtils.shareFile(requireContext(), item.filePath, item.ext)
    }

    private fun confirmDelete(item: DownloadItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_download))
            .setMessage(getString(R.string.delete_download_confirm))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteDownload(item)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun retryDownload(item: DownloadItem) {
        viewModel.retryDownload(item)
        requireContext().showToast(getString(R.string.download_restarted))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
