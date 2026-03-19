package com.android.zubanx.feature.onboarding

import androidx.lifecycle.viewModelScope
import com.android.zubanx.core.mvi.BaseViewModel
import com.android.zubanx.data.local.datastore.AppPreferences
import kotlinx.coroutines.launch

/**
 * ViewModel for [OnboardingFragment].
 *
 * Handles skip and done gestures by persisting `onboardingComplete = true`
 * and emitting [OnboardingContract.Effect.NavigateToHome].
 *
 * Page tracking is maintained in [OnboardingContract.State.currentPage] so the
 * Fragment can show "Next" vs "Done" on the last page.
 */
class OnboardingViewModel(
    private val prefs: AppPreferences
) : BaseViewModel<OnboardingContract.State, OnboardingContract.Event, OnboardingContract.Effect>(
    OnboardingContract.State()
) {
    override fun onEvent(event: OnboardingContract.Event) {
        when (event) {
            OnboardingContract.Event.DoneClicked -> completeOnboarding()
            OnboardingContract.Event.SkipClicked -> completeOnboarding()
            is OnboardingContract.Event.PageChanged -> setState { copy(currentPage = event.page) }
        }
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            prefs.setOnboardingComplete(true)
            sendEffect(OnboardingContract.Effect.NavigateToHome)
        }
    }
}
