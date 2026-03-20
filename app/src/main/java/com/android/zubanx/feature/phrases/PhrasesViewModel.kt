package com.android.zubanx.feature.phrases

import com.android.zubanx.core.mvi.BaseViewModel
import com.android.zubanx.feature.phrases.data.PhrasesData

class PhrasesViewModel : BaseViewModel<PhrasesContract.State, PhrasesContract.Event, PhrasesContract.Effect>(
    PhrasesContract.State.Active(categories = PhrasesData.categories)
) {
    override fun onEvent(event: PhrasesContract.Event) {
        when (event) {
            is PhrasesContract.Event.CategorySelected ->
                sendEffect(PhrasesContract.Effect.NavigateToCategory(event.category.id))
        }
    }
}
