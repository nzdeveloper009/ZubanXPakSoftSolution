package com.android.zubanx.feature.idioms

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.core.utils.collectFlow
import com.android.zubanx.databinding.FragmentIdiomsBinding
import com.android.zubanx.databinding.ItemIdiomCategoryBinding
import com.android.zubanx.feature.idioms.data.IdiomCategory
import org.koin.androidx.viewmodel.ext.android.viewModel

class IdiomsFragment : BaseFragment<FragmentIdiomsBinding>(FragmentIdiomsBinding::inflate) {

    private val viewModel: IdiomsViewModel by viewModel()

    private val adapter = CategoryAdapter { category ->
        viewModel.onEvent(IdiomsContract.Event.CategorySelected(category))
    }

    override fun setupViews() {
        binding.rvCategories.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvCategories.adapter = adapter
    }

    override fun observeState() {
        collectFlow(viewModel.state) { state ->
            if (state is IdiomsContract.State.Active) {
                adapter.submitList(state.categories)
            }
        }
        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                is IdiomsContract.Effect.NavigateToCategory ->
                    findNavController().navigate(
                        IdiomsFragmentDirections.actionIdiomsToCategory(effect.categoryId)
                    )
            }
        }
    }

    class CategoryAdapter(
        private val onClick: (IdiomCategory) -> Unit
    ) : ListAdapter<IdiomCategory, CategoryAdapter.VH>(DIFF) {

        inner class VH(val b: ItemIdiomCategoryBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemIdiomCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val cat = getItem(position)
            holder.b.ivCategoryIcon.setImageResource(cat.iconRes)
            holder.b.tvCategoryName.text = cat.displayName
            holder.b.cardCategory.setOnClickListener { onClick(cat) }
        }

        companion object {
            val DIFF = object : DiffUtil.ItemCallback<IdiomCategory>() {
                override fun areItemsTheSame(a: IdiomCategory, b: IdiomCategory) = a.id == b.id
                override fun areContentsTheSame(a: IdiomCategory, b: IdiomCategory) = a == b
            }
        }
    }
}
