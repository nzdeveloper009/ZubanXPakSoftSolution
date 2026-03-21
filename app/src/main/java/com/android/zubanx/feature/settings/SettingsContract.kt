package com.android.zubanx.feature.settings

import com.android.zubanx.core.mvi.UiEffect
import com.android.zubanx.core.mvi.UiEvent
import com.android.zubanx.core.mvi.UiState
import com.android.zubanx.domain.model.AiTone

object SettingsContract {
    data class State(
        val isPremium: Boolean = false,
        val aiTone: AiTone = AiTone.ORIGINAL,
        val offlineMode: Boolean = false,
        val floatingOverlay: Boolean = false,
        val autoSpeak: Boolean = false,
        val appVersion: String = ""
    ) : UiState

    sealed class Event : UiEvent {
        object NavigateToPremium : Event()
        object NavigateToHistory : Event()
        object NavigateToFavourites : Event()
        object ShowAiTonePicker : Event()
        data class SetAiTone(val tone: AiTone) : Event()
        object NavigateToLanguage : Event()
        object OpenPrivacyPolicy : Event()
        object OpenTerms : Event()
        object RateUs : Event()
        object ShareApp : Event()
        object ContactSupport : Event()
        data class SetOfflineMode(val enabled: Boolean) : Event()
        data class SetFloatingOverlay(val enabled: Boolean) : Event()
        data class SetAutoSpeak(val enabled: Boolean) : Event()
    }

    sealed class Effect : UiEffect {
        data class Navigate(val actionId: Int) : Effect()
        data class OpenUrl(val url: String) : Effect()
        object LaunchShare : Effect()
        object LaunchRateUs : Effect()
        object LaunchContactSupport : Effect()
        data class StartFloatingService(val enable: Boolean) : Effect()
        data class ShowToast(val message: String) : Effect()
        data class ShowAiToneDialog(val currentTone: AiTone) : Effect()
    }
}
