package com.android.zubanx.feature.dictionary

import com.android.zubanx.core.mvi.UiEffect
import com.android.zubanx.core.mvi.UiEvent
import com.android.zubanx.core.mvi.UiState
import com.android.zubanx.domain.model.DictionaryEntry

object DictionaryContract {

    sealed interface State : UiState {
        data object Idle : State
        data object Searching : State
        data class Success(
            val entry: DictionaryEntry,
            val history: List<DictionaryEntry> = emptyList()
        ) : State
        data class Error(
            val message: String,
            val history: List<DictionaryEntry> = emptyList()
        ) : State
    }

    sealed class Event : UiEvent {
        data class QueryChanged(val text: String) : Event()
        data object SearchClicked : Event()
        data class MicResult(val text: String) : Event()
        data object ClearSearch : Event()
        data class HistoryItemClicked(val entry: DictionaryEntry) : Event()
        data class NavigateToDetail(val entry: DictionaryEntry) : Event()
    }

    sealed class Effect : UiEffect {
        data class ShowToast(val message: String) : Effect()
        data class OpenWordDetail(val entry: DictionaryEntry) : Effect()
        data class LaunchMic(val langCode: String?) : Effect()
    }
}
