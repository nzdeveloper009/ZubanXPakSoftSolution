package com.android.zubanx.feature.phrases

import com.android.zubanx.core.mvi.UiEffect
import com.android.zubanx.core.mvi.UiEvent
import com.android.zubanx.core.mvi.UiState
import com.android.zubanx.feature.phrases.data.PhraseCategory

object PhrasesContract {

    sealed interface State : UiState {
        data class Active(
            val categories: List<PhraseCategory>
        ) : State
    }

    sealed class Event : UiEvent {
        data class CategorySelected(val category: PhraseCategory) : Event()
    }

    sealed class Effect : UiEffect {
        data class NavigateToCategory(val categoryId: String) : Effect()
    }
}
