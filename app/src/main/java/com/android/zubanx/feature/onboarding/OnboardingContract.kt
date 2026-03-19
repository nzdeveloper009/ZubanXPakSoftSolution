package com.android.zubanx.feature.onboarding

import com.android.zubanx.core.mvi.UiEffect
import com.android.zubanx.core.mvi.UiEvent
import com.android.zubanx.core.mvi.UiState

object OnboardingContract {

    data class State(
        val currentPage: Int = 0,
        val totalPages: Int = 3
    ) : UiState

    sealed class Event : UiEvent {
        /** User tapped the Skip button on any page. */
        data object SkipClicked : Event()

        /** User tapped the Done button on the last page. */
        data object DoneClicked : Event()

        /** ViewPager page changed. */
        data class PageChanged(val page: Int) : Event()
    }

    sealed class Effect : UiEffect {
        /** Navigate to the main Translate screen and clear the onboarding back stack. */
        data object NavigateToHome : Effect()
    }
}
