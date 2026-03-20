package com.android.zubanx.feature.favourite

import com.android.zubanx.core.mvi.UiEffect
import com.android.zubanx.core.mvi.UiEvent
import com.android.zubanx.core.mvi.UiState
import com.android.zubanx.domain.model.Favourite

object FavouriteContract {
    enum class Tab { TRANSLATE, DICTIONARY }

    data class Active(
        val translateItems: List<Favourite> = emptyList(),
        val dictionaryItems: List<Favourite> = emptyList(),
        val selectedTab: Tab = Tab.TRANSLATE
    ) : UiState

    sealed class Event : UiEvent {
        data class TabSelected(val tab: Tab) : Event()
        data class RequestDelete(val id: Long) : Event()
        data class DeleteConfirmed(val id: Long) : Event()
        data class ItemClicked(val item: Favourite) : Event()
        data class SpeakText(val text: String, val lang: String) : Event()
        data class CopyText(val text: String) : Event()
        data class ShareText(val text: String) : Event()
    }

    sealed class Effect : UiEffect {
        data class ConfirmDelete(val id: Long) : Effect()
        data class OpenTranslateDetail(val item: Favourite) : Effect()
        data class NavigateToWordDetail(val word: String, val language: String) : Effect()
        data class CopyToClipboard(val text: String) : Effect()
        data class Share(val text: String) : Effect()
        data class Speak(val text: String, val lang: String) : Effect()
        data class ShowToast(val message: String) : Effect()
    }
}
