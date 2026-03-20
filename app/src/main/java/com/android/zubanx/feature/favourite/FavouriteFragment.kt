package com.android.zubanx.feature.favourite

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.core.utils.collectFlow
import com.android.zubanx.core.utils.toast
import com.android.zubanx.databinding.FragmentFavouriteBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel

class FavouriteFragment : BaseFragment<FragmentFavouriteBinding>(FragmentFavouriteBinding::inflate) {

    private val viewModel: FavouriteViewModel by viewModel()
    private lateinit var translateAdapter: FavouriteAdapter
    private lateinit var dictionaryAdapter: FavouriteAdapter

    override fun setupViews() {
        translateAdapter = FavouriteAdapter { viewModel.onEvent(FavouriteContract.Event.ItemClicked(it)) }
        dictionaryAdapter = FavouriteAdapter { viewModel.onEvent(FavouriteContract.Event.ItemClicked(it)) }

        binding.rvTranslate.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTranslate.adapter = translateAdapter

        binding.rvDictionary.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDictionary.adapter = dictionaryAdapter

        setupSwipeToDelete(binding.rvTranslate, translateAdapter)
        setupSwipeToDelete(binding.rvDictionary, dictionaryAdapter)

        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                val selected = if (tab?.position == 0) FavouriteContract.Tab.TRANSLATE
                               else FavouriteContract.Tab.DICTIONARY
                viewModel.onEvent(FavouriteContract.Event.TabSelected(selected))
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun setupSwipeToDelete(recyclerView: RecyclerView, adapter: FavouriteAdapter) {
        val callback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.bindingAdapterPosition
                if (pos == RecyclerView.NO_ID.toInt()) return
                val item = adapter.currentList[pos]
                adapter.notifyItemChanged(pos) // restore immediately
                viewModel.onEvent(FavouriteContract.Event.RequestDelete(item.id))
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
    }

    override fun observeState() {
        collectFlow(viewModel.state) { state ->
            translateAdapter.submitList(state.translateItems)
            dictionaryAdapter.submitList(state.dictionaryItems)

            val isTranslate = state.selectedTab == FavouriteContract.Tab.TRANSLATE
            binding.rvTranslate.isVisible = isTranslate
            binding.rvDictionary.isVisible = !isTranslate
            binding.tvEmptyTranslate.isVisible = isTranslate && state.translateItems.isEmpty()
            binding.tvEmptyDictionary.isVisible = !isTranslate && state.dictionaryItems.isEmpty()

            val targetTab = if (isTranslate) 0 else 1
            if (binding.tabLayout.selectedTabPosition != targetTab) {
                binding.tabLayout.getTabAt(targetTab)?.select()
            }
        }

        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                is FavouriteContract.Effect.ConfirmDelete -> showDeleteDialog(effect.id)
                is FavouriteContract.Effect.OpenTranslateDetail -> {
                    FavouriteDetailBottomSheet.newInstance(effect.item)
                        .show(childFragmentManager, "favourite_detail")
                }
                is FavouriteContract.Effect.NavigateToWordDetail -> {
                    // Navigation to WordDetailFragment via nav component
                    // For now show a toast — nav action wired in Task 7
                }
                is FavouriteContract.Effect.CopyToClipboard -> {
                    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("favourite", effect.text))
                    requireContext().toast("Copied")
                }
                is FavouriteContract.Effect.Share -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, effect.text)
                    }
                    startActivity(Intent.createChooser(intent, "Share"))
                }
                is FavouriteContract.Effect.Speak -> Unit // Handled in BottomSheet
                is FavouriteContract.Effect.ShowToast -> requireContext().toast(effect.message)
            }
        }
    }

    private fun showDeleteDialog(id: Long) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete favourite?")
            .setMessage("This item will be removed from your favourites.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.onEvent(FavouriteContract.Event.DeleteConfirmed(id))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
