package com.android.zubanx.feature.phrases

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.zubanx.databinding.ItemPhraseBinding

data class PhraseItem(
    val index: Int,
    val displayText: String,
    val translatedText: String?,
    val isExpanded: Boolean,
    val isLoading: Boolean,
    val isError: Boolean
)

class PhrasesCategoryAdapter(
    private val onExpand: (Int) -> Unit,
    private val onSpeak: (Int) -> Unit,
    private val onCopy: (Int) -> Unit,
    private val onZoom: (String, String) -> Unit,    // translatedText, langCode
    private val onRetry: (Int) -> Unit
) : ListAdapter<PhraseItem, PhrasesCategoryAdapter.VH>(DIFF) {

    private var targetLangCode: String = "ur"

    fun updateTargetLang(code: String) {
        targetLangCode = code
    }

    inner class VH(val b: ItemPhraseBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemPhraseBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.b.tvPhrase.text = item.displayText

        holder.b.rowCollapsed.setOnClickListener { onExpand(item.index) }
        holder.b.btnExpand.setOnClickListener { onExpand(item.index) }

        holder.b.layoutExpanded.isVisible = item.isExpanded
        holder.b.progressTranslation.isVisible = item.isExpanded && item.isLoading
        holder.b.tvTranslated.isVisible = item.isExpanded && !item.isLoading && !item.isError && item.translatedText != null
        holder.b.layoutError.isVisible = item.isExpanded && item.isError
        holder.b.layoutActions.isVisible = item.isExpanded && !item.isLoading && !item.isError && item.translatedText != null

        item.translatedText?.let { translated ->
            holder.b.tvTranslated.text = translated
            holder.b.btnSpeak.setOnClickListener { onSpeak(item.index) }
            holder.b.btnCopy.setOnClickListener { onCopy(item.index) }
            holder.b.btnZoom.setOnClickListener { onZoom(translated, targetLangCode) }
        }
        holder.b.btnRetry.setOnClickListener { onRetry(item.index) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<PhraseItem>() {
            override fun areItemsTheSame(a: PhraseItem, b: PhraseItem) = a.index == b.index
            override fun areContentsTheSame(a: PhraseItem, b: PhraseItem) = a == b
        }
    }
}
