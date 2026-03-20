package com.android.zubanx.feature.phrases

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.core.utils.collectFlow
import com.android.zubanx.databinding.FragmentPhrasesBinding
import com.android.zubanx.databinding.ItemPhraseCategoryBinding
import com.android.zubanx.feature.phrases.data.PhraseCategory
import org.koin.androidx.viewmodel.ext.android.viewModel

class PhrasesFragment : BaseFragment<FragmentPhrasesBinding>(FragmentPhrasesBinding::inflate) {

    private val viewModel: PhrasesViewModel by viewModel()

    private val adapter = CategoryAdapter { category ->
        viewModel.onEvent(PhrasesContract.Event.CategorySelected(category))
    }

    override fun setupViews() {
        binding.rvCategories.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvCategories.adapter = adapter
    }

    override fun observeState() {
        collectFlow(viewModel.state) { state ->
            if (state is PhrasesContract.State.Active) {
                adapter.submitList(state.categories)
            }
        }
        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                is PhrasesContract.Effect.NavigateToCategory ->
                    findNavController().navigate(
                        PhrasesFragmentDirections.actionPhrasesToCategory(effect.categoryId)
                    )
            }
        }
    }

    class CategoryAdapter(
        private val onClick: (PhraseCategory) -> Unit
    ) : ListAdapter<PhraseCategory, CategoryAdapter.VH>(DIFF) {

        inner class VH(val b: ItemPhraseCategoryBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemPhraseCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val cat = getItem(position)
            holder.b.ivCategoryIcon.setImageResource(cat.iconRes)
            holder.b.tvCategoryName.text = cat.displayName
            holder.b.cardCategory.setOnClickListener { onClick(cat) }
        }

        companion object {
            val DIFF = object : DiffUtil.ItemCallback<PhraseCategory>() {
                override fun areItemsTheSame(a: PhraseCategory, b: PhraseCategory) = a.id == b.id
                override fun areContentsTheSame(a: PhraseCategory, b: PhraseCategory) = a == b
            }
        }
    }
}
