package com.android.zubanx.feature.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.zubanx.databinding.FragmentOnboardingPageBinding

/**
 * ViewPager2 adapter for the static onboarding pages.
 *
 * [pages] is a list of (title, description) pairs provided by [OnboardingFragment].
 */
class OnboardingPagerAdapter(
    private val pages: List<Pair<String, String>>
) : RecyclerView.Adapter<OnboardingPagerAdapter.PageViewHolder>() {

    inner class PageViewHolder(
        private val binding: FragmentOnboardingPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(title: String, description: String) {
            binding.tvPageTitle.text = title
            binding.tvPageDescription.text = description
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = FragmentOnboardingPageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val (title, description) = pages[position]
        holder.bind(title, description)
    }

    override fun getItemCount(): Int = pages.size
}
