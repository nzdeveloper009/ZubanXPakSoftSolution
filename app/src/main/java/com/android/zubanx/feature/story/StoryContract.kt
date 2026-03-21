package com.android.zubanx.feature.story

import com.android.zubanx.core.mvi.UiEffect
import com.android.zubanx.core.mvi.UiEvent
import com.android.zubanx.core.mvi.UiState
import com.android.zubanx.feature.story.data.StoryCategory

object StoryContract {
    sealed interface State : UiState {
        data class Active(val categories: List<StoryCategory>) : State
    }
    sealed class Event : UiEvent {
        data class CategorySelected(val category: StoryCategory) : Event()
    }
    sealed class Effect : UiEffect {
        data class NavigateToCategory(val categoryId: String) : Effect()
    }
}
