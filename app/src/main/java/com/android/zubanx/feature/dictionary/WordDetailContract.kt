package com.android.zubanx.feature.dictionary

import com.android.zubanx.core.mvi.UiEffect
import com.android.zubanx.core.mvi.UiEvent
import com.android.zubanx.core.mvi.UiState
import com.android.zubanx.domain.model.DictionaryEntry

object WordDetailContract {

    sealed interface State : UiState {
        data class Loaded(
            val entry: DictionaryEntry,
            val aiInsight: String? = null,
            val aiLoading: Boolean = false,
            val isFavourite: Boolean = false
        ) : State
        data class Error(val message: String) : State
    }

    sealed class Event : UiEvent {
        data class Load(val entry: DictionaryEntry) : Event()
        data class EnrichWithAi(val expert: String) : Event()
        data object SpeakWord : Event()
        data object CopyDefinition : Event()
        data object ToggleFavourite : Event()
    }

    sealed class Effect : UiEffect {
        data class ShowToast(val message: String) : Effect()
        data class SpeakText(val text: String, val langCode: String) : Effect()
        data class CopyToClipboard(val text: String) : Effect()
        data object ShowFavourited : Effect()
    }
}
