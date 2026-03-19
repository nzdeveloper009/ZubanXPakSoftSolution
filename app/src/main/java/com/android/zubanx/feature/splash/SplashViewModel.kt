package com.android.zubanx.feature.splash

import androidx.lifecycle.viewModelScope
import com.android.zubanx.core.mvi.BaseViewModel
import com.android.zubanx.data.local.datastore.AppPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val SPLASH_DELAY_MS = 2_000L

/**
 * ViewModel for [SplashFragment].
 *
 * On [SplashContract.Event.Init], waits [SPLASH_DELAY_MS] ms (for the Lottie animation),
 * reads [AppPreferences.onboardingComplete] once, and emits either:
 * - [SplashContract.Effect.NavigateToOnboarding] (first launch)
 * - [SplashContract.Effect.NavigateToHome] (returning user)
 *
 * The ViewModel does NOT navigate — it emits a one-shot Effect. The Fragment navigates.
 */
class SplashViewModel(
    private val prefs: AppPreferences
) : BaseViewModel<SplashContract.State, SplashContract.Event, SplashContract.Effect>(
    SplashContract.State
) {
    override fun onEvent(event: SplashContract.Event) {
        when (event) {
            SplashContract.Event.Init -> handleInit()
        }
    }

    private fun handleInit() {
        viewModelScope.launch {
            delay(SPLASH_DELAY_MS)
            val isOnboarded = prefs.onboardingComplete.first()
            sendEffect(
                if (isOnboarded) SplashContract.Effect.NavigateToHome
                else SplashContract.Effect.NavigateToOnboarding
            )
        }
    }
}
