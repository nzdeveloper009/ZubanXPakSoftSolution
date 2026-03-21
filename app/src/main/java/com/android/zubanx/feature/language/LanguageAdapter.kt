package com.android.zubanx.feature.language

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.zubanx.R
import com.android.zubanx.databinding.ItemLanguageBinding
import com.google.android.material.color.MaterialColors
import androidx.appcompat.R as AppCompatR

class LanguageAdapter(
    private val onItemClick: (AppLanguage) -> Unit
) : ListAdapter<AppLanguage, LanguageAdapter.ViewHolder>(DIFF_CALLBACK) {

    private var selectedCode: String = "en"

    fun setSelectedCode(code: String) {
        val prev = selectedCode
        selectedCode = code
        // Notify only the two affected items (deselected + newly selected) for efficiency.
        val prevIndex = currentList.indexOfFirst { it.code == prev }
        val newIndex = currentList.indexOfFirst { it.code == code }
        if (prevIndex >= 0) notifyItemChanged(prevIndex)
        if (newIndex >= 0 && newIndex != prevIndex) notifyItemChanged(newIndex)
    }

    inner class ViewHolder(private val binding: ItemLanguageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AppLanguage) {
            binding.imgFlag.setImageResource(item.flagDrawable)
            binding.tvLanguageName.text = item.displayName

            val isSelected = item.code == selectedCode
            val checkIcon = if (isSelected) R.drawable.ic_check_circle else R.drawable.ic_radio_button_unchecked
            binding.imgSelected.setImageResource(checkIcon)

            val tint = if (isSelected)
                MaterialColors.getColor(
                    binding.root, AppCompatR.attr.colorPrimary)
            else
                MaterialColors.getColor(
                    binding.root, com.google.android.material.R.attr.colorOutline)
            binding.imgSelected.setColorFilter(tint)

            val bgColor = if (isSelected)
                MaterialColors.getColor(
                    binding.root, com.google.android.material.R.attr.colorSurfaceVariant)
            else
                android.graphics.Color.TRANSPARENT
            binding.root.setBackgroundColor(bgColor)

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AppLanguage>() {
            override fun areItemsTheSame(oldItem: AppLanguage, newItem: AppLanguage) =
                oldItem.code == newItem.code
            override fun areContentsTheSame(oldItem: AppLanguage, newItem: AppLanguage) =
                oldItem == newItem
        }
    }
}
