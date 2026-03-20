package com.android.zubanx.feature.translate

import com.android.zubanx.core.mvi.UiEffect
import com.android.zubanx.core.mvi.UiEvent
import com.android.zubanx.core.mvi.UiState
import com.android.zubanx.domain.model.Translation

object TranslateContract {

    sealed interface State : UiState {
        data object Idle : State
        data object Translating : State
        data class Success(
            val inputText: String,
            val translatedText: String,
            val sourceLang: LanguageItem,
            val targetLang: LanguageItem,
            val expert: String,
            val history: List<Translation> = emptyList()
        ) : State
        data class Error(
            val message: String,
            val inputText: String = "",
            val history: List<Translation> = emptyList()
        ) : State
    }

    sealed class Event : UiEvent {
        data class InputChanged(val text: String) : Event()
        data object TranslateClicked : Event()
        data class SourceLangSelected(val language: LanguageItem) : Event()
        data class TargetLangSelected(val language: LanguageItem) : Event()
        data object SwapLanguages : Event()
        data class MicResult(val text: String) : Event()
        data object ClearInput : Event()
        data object CopyTranslation : Event()
        data object SpeakTranslation : Event()
        data object AddToFavourites : Event()
        data object ShareTranslation : Event()
        data class HistoryItemClicked(val translation: Translation) : Event()
        data class DeleteHistoryItem(val id: Long) : Event()
    }

    sealed class Effect : UiEffect {
        data class ShowToast(val message: String) : Effect()
        data class CopyToClipboard(val text: String) : Effect()
        data class SpeakText(val text: String, val langCode: String) : Effect()
        data class ShareText(val text: String) : Effect()
        data class LaunchMic(val sourceCode: String?) : Effect()
    }
}
