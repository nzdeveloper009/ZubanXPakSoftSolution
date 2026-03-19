package com.android.zubanx.feature.splash

import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.android.zubanx.R
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.core.utils.collectFlow
import com.android.zubanx.databinding.FragmentSplashBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Splash screen — shown on every app launch.
 *
 * Plays a Lottie animation while [SplashViewModel] waits 2 seconds and checks
 * whether onboarding has been completed. Navigates based on the emitted Effect.
 *
 * This destination is removed from the back stack via `popUpToInclusive = true`
 * so the user cannot press Back to return to it.
 */
class SplashFragment : BaseFragment<FragmentSplashBinding>(FragmentSplashBinding::inflate) {

    private val viewModel: SplashViewModel by viewModel()

    override fun setupViews() {
        viewModel.onEvent(SplashContract.Event.Init)
    }

    override fun observeState() {
        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                SplashContract.Effect.NavigateToOnboarding ->
                    findNavController().navigate(R.id.action_splash_to_onboarding)
                SplashContract.Effect.NavigateToHome ->
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
    }
}
