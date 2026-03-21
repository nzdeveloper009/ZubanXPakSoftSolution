package com.android.zubanx.feature.story

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.core.utils.collectFlow
import com.android.zubanx.databinding.FragmentStoryBinding
import com.android.zubanx.databinding.ItemStoryCategoryBinding
import com.android.zubanx.feature.story.data.StoryCategory
import org.koin.androidx.viewmodel.ext.android.viewModel

class StoryFragment : BaseFragment<FragmentStoryBinding>(FragmentStoryBinding::inflate) {

    private val viewModel: StoryViewModel by viewModel()

    private val adapter = CategoryAdapter { category ->
        viewModel.onEvent(StoryContract.Event.CategorySelected(category))
    }

    override fun setupViews() {
        binding.rvCategories.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvCategories.adapter = adapter
    }

    override fun observeState() {
        collectFlow(viewModel.state) { state ->
            if (state is StoryContract.State.Active) adapter.submitList(state.categories)
        }
        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                is StoryContract.Effect.NavigateToCategory ->
                    findNavController().navigate(
                        StoryFragmentDirections.actionStoryToCategory(effect.categoryId)
                    )
            }
        }
    }

    class CategoryAdapter(
        private val onClick: (StoryCategory) -> Unit
    ) : ListAdapter<StoryCategory, CategoryAdapter.VH>(DIFF) {

        inner class VH(val b: ItemStoryCategoryBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemStoryCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val cat = getItem(position)
            holder.b.ivCategoryIcon.setImageResource(cat.iconRes)
            holder.b.tvCategoryName.text = cat.displayName
            holder.b.cardCategory.setOnClickListener { onClick(cat) }
        }

        companion object {
            val DIFF = object : DiffUtil.ItemCallback<StoryCategory>() {
                override fun areItemsTheSame(a: StoryCategory, b: StoryCategory) = a.id == b.id
                override fun areContentsTheSame(a: StoryCategory, b: StoryCategory) = a == b
            }
        }
    }
}
