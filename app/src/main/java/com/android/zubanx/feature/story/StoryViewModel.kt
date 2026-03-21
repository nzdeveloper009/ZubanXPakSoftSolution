package com.android.zubanx.feature.story

import com.android.zubanx.core.mvi.BaseViewModel
import com.android.zubanx.feature.story.data.StoryCategory

class StoryViewModel : BaseViewModel<StoryContract.State, StoryContract.Event, StoryContract.Effect>(
    StoryContract.State.Active(StoryCategory.ALL)
) {
    override fun onEvent(event: StoryContract.Event) {
        when (event) {
            is StoryContract.Event.CategorySelected ->
                sendEffect(StoryContract.Effect.NavigateToCategory(event.category.id))
        }
    }
}
