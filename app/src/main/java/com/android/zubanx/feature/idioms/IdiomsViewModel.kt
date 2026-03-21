package com.android.zubanx.feature.idioms

import com.android.zubanx.core.mvi.BaseViewModel
import com.android.zubanx.feature.idioms.data.IdiomCategory

class IdiomsViewModel : BaseViewModel<IdiomsContract.State, IdiomsContract.Event, IdiomsContract.Effect>(
    IdiomsContract.State.Active(IdiomCategory.ALL)
) {
    override fun onEvent(event: IdiomsContract.Event) {
        when (event) {
            is IdiomsContract.Event.CategorySelected ->
                sendEffect(IdiomsContract.Effect.NavigateToCategory(event.category.id))
        }
    }
}
