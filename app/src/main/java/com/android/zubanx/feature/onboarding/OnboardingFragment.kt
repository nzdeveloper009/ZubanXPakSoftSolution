package com.android.zubanx.feature.onboarding

import androidx.activity.OnBackPressedCallback
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.android.zubanx.R
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.core.utils.collectFlow
import com.android.zubanx.databinding.FragmentOnboardingBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Onboarding flow — shown only on first launch.
 *
 * Three static pages are displayed in a [ViewPager2]. The Skip button is always
 * visible. The Next button reads "Done" on the last page. Either Skip or Done
 * triggers [OnboardingContract.Event.SkipClicked]/[OnboardingContract.Event.DoneClicked],
 * which persists `onboardingComplete = true` and navigates to the main screen.
 *
 * Back press while on a non-first page scrolls the pager back one page.
 * Back press on page 0 does nothing (default back behaviour is suppressed to prevent
 * returning to the splash screen).
 */
class OnboardingFragment : BaseFragment<FragmentOnboardingBinding>(FragmentOnboardingBinding::inflate) {

    private val viewModel: OnboardingViewModel by viewModel()

    private val pages = listOf(
        "Translate Instantly" to "Translate text, speech, and images across 100+ languages in real-time.",
        "AI Expert Mode" to "Choose your AI expert — GPT, Gemini, Claude, or our built-in model.",
        "Works Offline" to "Download language packs and translate without an internet connection."
    )

    private val adapter by lazy { OnboardingPagerAdapter(pages) }

    override fun setupViews() {
        binding.viewPager.adapter = adapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.onEvent(OnboardingContract.Event.PageChanged(position))
                binding.btnNext.text = if (position == pages.lastIndex) "Done" else getString(R.string.btn_next)
            }
        })

        binding.btnSkip.setOnClickListener {
            viewModel.onEvent(OnboardingContract.Event.SkipClicked)
        }

        binding.btnNext.setOnClickListener {
            val currentPage = viewModel.state.value.currentPage
            if (currentPage < pages.lastIndex) {
                binding.viewPager.currentItem = currentPage + 1
            } else {
                viewModel.onEvent(OnboardingContract.Event.DoneClicked)
            }
        }

        backPressHandler = {
            val currentPage = viewModel.state.value.currentPage
            if (currentPage > 0) {
                binding.viewPager.currentItem = currentPage - 1
                true
            } else {
                // Suppress back on all onboarding pages — user must Skip or complete onboarding
                true
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (backPressHandler?.invoke() != true) {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    override fun observeState() {
        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                OnboardingContract.Effect.NavigateToHome -> navigateToHome()
            }
        }
    }

    private fun navigateToHome() {
        findNavController().navigate(
            R.id.action_global_onboarding_to_translate,
            null,
            NavOptions.Builder()
                .setEnterAnim(R.anim.scale_fade_in)
                .setExitAnim(R.anim.fade_out)
                .setPopUpTo(R.id.nav_onboarding, inclusive = true)
                .build()
        )
    }
}
