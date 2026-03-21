package com.android.zubanx.feature.idioms

import com.android.zubanx.core.mvi.UiEffect
import com.android.zubanx.core.mvi.UiEvent
import com.android.zubanx.core.mvi.UiState
import com.android.zubanx.feature.idioms.data.IdiomCategory

object IdiomsContract {

    sealed interface State : UiState {
        data class Active(val categories: List<IdiomCategory>) : State
    }

    sealed class Event : UiEvent {
        data class CategorySelected(val category: IdiomCategory) : Event()
    }

    sealed class Effect : UiEffect {
        data class NavigateToCategory(val categoryId: String) : Effect()
    }
}
