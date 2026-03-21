package com.android.zubanx.feature.language

import com.android.zubanx.core.mvi.UiEffect
import com.android.zubanx.core.mvi.UiEvent
import com.android.zubanx.core.mvi.UiState

object LanguageContract {
    data class State(
        val selectedCode: String = "en"
    ) : UiState

    sealed class Event : UiEvent {
        data class SelectLanguage(val code: String) : Event()
        object Confirm : Event()
    }

    sealed class Effect : UiEffect {
        data class ApplyLocaleAndRestart(val code: String) : Effect()
    }
}
