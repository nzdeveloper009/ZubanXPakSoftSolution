package com.android.zubanx.feature.favourite

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.zubanx.databinding.ItemFavouriteBinding
import com.android.zubanx.domain.model.Favourite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FavouriteAdapter(
    private val onItemClick: (Favourite) -> Unit
) : ListAdapter<Favourite, FavouriteAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(private val binding: ItemFavouriteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Favourite) {
            binding.tvSourceText.text = item.sourceText
            binding.tvTranslatedText.text = item.translatedText
            binding.tvLangPair.text = "${item.sourceLang.uppercase()} → ${item.targetLang.uppercase()}"
            binding.tvTimestamp.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                .format(Date(item.timestamp))
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFavouriteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Favourite>() {
            override fun areItemsTheSame(oldItem: Favourite, newItem: Favourite) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Favourite, newItem: Favourite) = oldItem == newItem
        }
    }
}
