package com.android.zubanx.feature.phrases

import com.android.zubanx.core.mvi.UiEffect
import com.android.zubanx.core.mvi.UiEvent
import com.android.zubanx.core.mvi.UiState
import com.android.zubanx.feature.phrases.data.PhraseCategory
import com.android.zubanx.feature.translate.LanguageItem

object PhrasesCategoryContract {

    sealed interface State : UiState {
        data class Active(
            val category: PhraseCategory,
            val displayPhrases: List<String>,
            val langSource: LanguageItem = LanguageItem.fromCode("en"),
            val langTarget: LanguageItem = LanguageItem.fromCode("ur"),
            val expandedIndex: Int? = null,
            val translationCache: Map<String, String> = emptyMap(),
            val loadingIndices: Set<Int> = emptySet(),
            val errorIndices: Set<Int> = emptySet()
        ) : State
    }

    sealed class Event : UiEvent {
        data class ExpandPhrase(val index: Int) : Event()
        data object CollapsePhrase : Event()
        data class LangSourceSelected(val lang: LanguageItem) : Event()
        data class LangTargetSelected(val lang: LanguageItem) : Event()
        data object SwapLanguages : Event()
        data class RetryTranslation(val index: Int) : Event()
        data class SpeakPhrase(val index: Int) : Event()
        data class CopyPhrase(val index: Int) : Event()
    }

    sealed class Effect : UiEffect {
        data class SpeakText(val text: String, val langCode: String) : Effect()
        data class CopyToClipboard(val text: String) : Effect()
        data class ShowToast(val message: String) : Effect()
        data class NavigateToZoom(val translatedText: String, val langCode: String) : Effect()
    }
}
