package com.android.zubanx.feature.story

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.zubanx.databinding.ItemStoryBinding
import com.android.zubanx.feature.story.data.StoryEntry

data class StoryDisplayItem(
    val index: Int,
    val entry: StoryEntry,
    val translatedBody: String? = null,
    val isExpanded: Boolean = false,
    val isLoading: Boolean = false,
    val hasError: Boolean = false
)

class StoryCategoryAdapter(
    private val onExpand: (Int) -> Unit,
    private val onTranslate: (Int) -> Unit,
    private val onSpeak: (Int) -> Unit,
    private val onCopy: (Int) -> Unit,
    private val onRetry: (Int) -> Unit
) : ListAdapter<StoryDisplayItem, StoryCategoryAdapter.VH>(DIFF) {

    inner class VH(val b: ItemStoryBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemStoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val b = holder.b

        b.tvTitle.text = item.entry.title
        b.tvBody.text = item.entry.body

        val isExpanded = item.isExpanded
        b.llExpanded.visibility = if (isExpanded) View.VISIBLE else View.GONE
        b.ivArrow.rotation = if (isExpanded) 180f else 0f
        b.llHeader.setOnClickListener { onExpand(item.index) }

        // Translation states
        when {
            item.isLoading -> {
                b.tvTranslationLabel.visibility = View.GONE
                b.tvTranslation.visibility = View.GONE
                b.progressTranslation.visibility = View.VISIBLE
                b.tvError.visibility = View.GONE
            }
            item.hasError -> {
                b.tvTranslationLabel.visibility = View.GONE
                b.tvTranslation.visibility = View.GONE
                b.progressTranslation.visibility = View.GONE
                b.tvError.visibility = View.VISIBLE
                b.tvError.setOnClickListener { onRetry(item.index) }
            }
            item.translatedBody != null -> {
                b.tvTranslationLabel.visibility = View.VISIBLE
                b.tvTranslation.text = item.translatedBody
                b.tvTranslation.visibility = View.VISIBLE
                b.progressTranslation.visibility = View.GONE
                b.tvError.visibility = View.GONE
            }
            else -> {
                b.tvTranslationLabel.visibility = View.GONE
                b.tvTranslation.visibility = View.GONE
                b.progressTranslation.visibility = View.GONE
                b.tvError.visibility = View.GONE
            }
        }

        b.btnTranslate.setOnClickListener { onTranslate(item.index) }
        b.btnSpeak.setOnClickListener { onSpeak(item.index) }
        b.btnCopy.setOnClickListener { onCopy(item.index) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<StoryDisplayItem>() {
            override fun areItemsTheSame(a: StoryDisplayItem, b: StoryDisplayItem) = a.index == b.index
            override fun areContentsTheSame(a: StoryDisplayItem, b: StoryDisplayItem) = a == b
        }
    }
}
