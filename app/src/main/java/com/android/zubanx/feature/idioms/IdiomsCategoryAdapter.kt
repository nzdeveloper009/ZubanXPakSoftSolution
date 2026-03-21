package com.android.zubanx.feature.idioms

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.zubanx.databinding.ItemIdiomBinding
import com.android.zubanx.feature.idioms.data.IdiomEntry

data class IdiomDisplayItem(
    val index: Int,
    val entry: IdiomEntry,
    val translatedMeaning: String? = null,
    val isExpanded: Boolean = false,
    val isLoading: Boolean = false,
    val hasError: Boolean = false
)

class IdiomsCategoryAdapter(
    private val onExpand: (Int) -> Unit,
    private val onSpeak: (Int) -> Unit,
    private val onCopy: (Int) -> Unit,
    private val onRetry: (Int) -> Unit
) : ListAdapter<IdiomDisplayItem, IdiomsCategoryAdapter.VH>(DIFF) {

    inner class VH(val b: ItemIdiomBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemIdiomBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val b = holder.b

        b.tvTitle.text = item.entry.title
        b.tvExample.text = item.entry.example

        // Expand/collapse
        b.llExpanded.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
        b.ivArrow.rotation = if (item.isExpanded) 180f else 0f
        b.llHeader.setOnClickListener { onExpand(item.index) }

        // Meaning states
        when {
            item.isLoading -> {
                b.tvMeaning.visibility = View.GONE
                b.progressMeaning.visibility = View.VISIBLE
                b.tvError.visibility = View.GONE
            }
            item.hasError -> {
                b.tvMeaning.visibility = View.GONE
                b.progressMeaning.visibility = View.GONE
                b.tvError.visibility = View.VISIBLE
                b.tvError.setOnClickListener { onRetry(item.index) }
            }
            item.translatedMeaning != null -> {
                b.tvMeaning.text = item.translatedMeaning
                b.tvMeaning.visibility = View.VISIBLE
                b.progressMeaning.visibility = View.GONE
                b.tvError.visibility = View.GONE
            }
            else -> {
                // Not yet translated — show English meaning as fallback
                b.tvMeaning.text = item.entry.meaning
                b.tvMeaning.visibility = View.VISIBLE
                b.progressMeaning.visibility = View.GONE
                b.tvError.visibility = View.GONE
            }
        }

        b.btnSpeak.setOnClickListener { onSpeak(item.index) }
        b.btnCopy.setOnClickListener { onCopy(item.index) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<IdiomDisplayItem>() {
            override fun areItemsTheSame(a: IdiomDisplayItem, b: IdiomDisplayItem) = a.index == b.index
            override fun areContentsTheSame(a: IdiomDisplayItem, b: IdiomDisplayItem) = a == b
        }
    }
}
