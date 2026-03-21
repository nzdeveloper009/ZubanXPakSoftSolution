package com.android.zubanx.feature.story

import com.android.zubanx.core.mvi.UiEffect
import com.android.zubanx.core.mvi.UiEvent
import com.android.zubanx.core.mvi.UiState
import com.android.zubanx.feature.story.data.StoryCategory
import com.android.zubanx.feature.story.data.StoryEntry
import com.android.zubanx.feature.translate.LanguageItem

object StoryCategoryContract {

    sealed interface State : UiState {
        data class Active(
            val category: StoryCategory,
            val stories: List<StoryEntry>,
            val langSource: LanguageItem = LanguageItem.fromCode("en"),
            val langTarget: LanguageItem = LanguageItem.fromCode("ur"),
            val expandedIndex: Int? = null,
            // cache key: "sourceLang:targetLang:storyId" → translated body
            val translationCache: Map<String, String> = emptyMap(),
            val loadingIndices: Set<Int> = emptySet(),
            val errorIndices: Set<Int> = emptySet()
        ) : State
    }

    sealed class Event : UiEvent {
        data class ExpandStory(val index: Int) : Event()
        data object CollapseStory : Event()
        data class LangSourceSelected(val lang: LanguageItem) : Event()
        data class LangTargetSelected(val lang: LanguageItem) : Event()
        data object SwapLanguages : Event()
        data class TranslateStory(val index: Int) : Event() // explicit tap required; not fired automatically on expand
        data class RetryTranslation(val index: Int) : Event()
        data class SpeakStory(val index: Int) : Event()
        data class CopyStory(val index: Int) : Event()
    }

    sealed class Effect : UiEffect {
        data class SpeakText(val text: String, val langCode: String) : Effect()
        data class CopyToClipboard(val text: String) : Effect()
        data class ShowToast(val message: String) : Effect()
    }
}
