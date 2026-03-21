package com.android.zubanx.feature.idioms

import com.android.zubanx.core.mvi.UiEffect
import com.android.zubanx.core.mvi.UiEvent
import com.android.zubanx.core.mvi.UiState
import com.android.zubanx.feature.idioms.data.IdiomCategory
import com.android.zubanx.feature.idioms.data.IdiomEntry
import com.android.zubanx.feature.translate.LanguageItem

object IdiomsCategoryContract {

    sealed interface State : UiState {
        data class Active(
            val category: IdiomCategory,
            val idioms: List<IdiomEntry>,           // source English entries (never changes)
            val langSource: LanguageItem = LanguageItem.fromCode("en"),
            val langTarget: LanguageItem = LanguageItem.fromCode("ur"),
            val expandedIndex: Int? = null,
            // cache key: "sourceLang:targetLang:index" → translated meaning
            val translationCache: Map<String, String> = emptyMap(),
            val loadingIndices: Set<Int> = emptySet(),
            val errorIndices: Set<Int> = emptySet()
        ) : State
    }

    sealed class Event : UiEvent {
        data class ExpandIdiom(val index: Int) : Event()
        data object CollapseIdiom : Event()
        data class LangSourceSelected(val lang: LanguageItem) : Event()
        data class LangTargetSelected(val lang: LanguageItem) : Event()
        data object SwapLanguages : Event()
        data class RetryTranslation(val index: Int) : Event()
        data class SpeakIdiom(val index: Int) : Event()
        data class CopyIdiom(val index: Int) : Event()
    }

    sealed class Effect : UiEffect {
        data class SpeakText(val text: String, val langCode: String) : Effect()
        data class CopyToClipboard(val text: String) : Effect()
        data class ShowToast(val message: String) : Effect()
    }
}
