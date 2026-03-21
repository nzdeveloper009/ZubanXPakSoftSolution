package com.android.zubanx.feature.learn

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.zubanx.R
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.core.navigation.safeNavigate
import com.android.zubanx.databinding.FragmentLearnBinding
import com.android.zubanx.databinding.ItemLearnSectionBinding

data class LearnSection(
    val titleRes: Int,
    val iconRes: Int,
    val navigate: () -> Unit
)

class LearnFragment : BaseFragment<FragmentLearnBinding>(FragmentLearnBinding::inflate) {

    private lateinit var sections: List<LearnSection>
    private lateinit var sectionAdapter: SectionAdapter

    override fun setupViews() {
        sections = listOf(
            LearnSection(R.string.learn_section_phrases, R.drawable.ic_nav_phrases) {
                safeNavigate(LearnFragmentDirections.actionLearnToPhrases())
            },
            LearnSection(R.string.learn_section_idioms, R.drawable.ic_category_greeting) {
                // cross-graph: no Safe Args directions available for included graphs
                findNavController().navigate(R.id.nav_idioms)
            },
            LearnSection(R.string.learn_section_story, R.drawable.ic_category_hotel) {
                // cross-graph: no Safe Args directions available for included graphs
                findNavController().navigate(R.id.nav_story)
            }
        )

        sectionAdapter = SectionAdapter()
        binding.rvSections.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvSections.adapter = sectionAdapter
        sectionAdapter.submitList(sections)
    }

    class SectionAdapter : ListAdapter<LearnSection, SectionAdapter.VH>(DIFF) {

        inner class VH(val b: ItemLearnSectionBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemLearnSectionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val section = getItem(position)
            holder.b.ivSectionIcon.setImageResource(section.iconRes)
            holder.b.tvSectionName.setText(section.titleRes)
            holder.b.cardSection.setOnClickListener { section.navigate() }
        }

        companion object {
            val DIFF = object : DiffUtil.ItemCallback<LearnSection>() {
                override fun areItemsTheSame(a: LearnSection, b: LearnSection) =
                    a.titleRes == b.titleRes
                override fun areContentsTheSame(a: LearnSection, b: LearnSection) =
                    a.titleRes == b.titleRes && a.iconRes == b.iconRes
            }
        }
    }
}
