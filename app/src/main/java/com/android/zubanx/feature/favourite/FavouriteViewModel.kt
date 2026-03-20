package com.android.zubanx.feature.favourite

import androidx.lifecycle.viewModelScope
import com.android.zubanx.core.mvi.BaseViewModel
import com.android.zubanx.domain.model.FavouriteCategory
import com.android.zubanx.domain.usecase.favourite.DeleteFavouriteUseCase
import com.android.zubanx.domain.usecase.favourite.GetFavouritesByCategoryUseCase
import kotlinx.coroutines.launch

class FavouriteViewModel(
    private val getFavouritesByCategory: GetFavouritesByCategoryUseCase,
    private val deleteFavourite: DeleteFavouriteUseCase
) : BaseViewModel<FavouriteContract.Active, FavouriteContract.Event, FavouriteContract.Effect>(
    FavouriteContract.Active()
) {
    init {
        viewModelScope.launch {
            getFavouritesByCategory(FavouriteCategory.TRANSLATE).collect { items ->
                setState { copy(translateItems = items) }
            }
        }
        viewModelScope.launch {
            getFavouritesByCategory(FavouriteCategory.DICTIONARY).collect { items ->
                setState { copy(dictionaryItems = items) }
            }
        }
    }

    override fun onEvent(event: FavouriteContract.Event) {
        when (event) {
            is FavouriteContract.Event.TabSelected -> setState { copy(selectedTab = event.tab) }
            is FavouriteContract.Event.RequestDelete -> sendEffect(FavouriteContract.Effect.ConfirmDelete(event.id))
            is FavouriteContract.Event.DeleteConfirmed -> viewModelScope.launch {
                deleteFavourite(event.id)
            }
            is FavouriteContract.Event.ItemClicked -> {
                val item = event.item
                if (item.category == FavouriteCategory.DICTIONARY) {
                    sendEffect(FavouriteContract.Effect.NavigateToWordDetail(item.sourceText, item.sourceLang))
                } else {
                    sendEffect(FavouriteContract.Effect.OpenTranslateDetail(item))
                }
            }
            is FavouriteContract.Event.SpeakText -> sendEffect(FavouriteContract.Effect.Speak(event.text, event.lang))
            is FavouriteContract.Event.CopyText -> sendEffect(FavouriteContract.Effect.CopyToClipboard(event.text))
            is FavouriteContract.Event.ShareText -> sendEffect(FavouriteContract.Effect.Share(event.text))
        }
    }
}
