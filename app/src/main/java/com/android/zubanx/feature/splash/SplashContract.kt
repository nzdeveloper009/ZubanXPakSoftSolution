package com.android.zubanx.feature.splash

import com.android.zubanx.core.mvi.UiEffect
import com.android.zubanx.core.mvi.UiEvent
import com.android.zubanx.core.mvi.UiState

object SplashContract {

    /**
     * The splash screen has one meaningful state — the animation is playing.
     * No Loading/Success/Error distinction is needed here.
     */
    data object State : UiState

    sealed class Event : UiEvent {
        /** Sent by the Fragment when the view is created; triggers the route-check coroutine. */
        data object Init : Event()
    }

    sealed class Effect : UiEffect {
        /** Navigate to the Onboarding screen (first launch). */
        data object NavigateToOnboarding : Effect()

        /** Navigate to the main Translate screen (returning user). */
        data object NavigateToHome : Effect()
    }
}
