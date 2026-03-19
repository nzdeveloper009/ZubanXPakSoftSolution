package com.android.zubanx.core.mvi

import org.junit.Test

class MviContractTest {

    private sealed interface State : UiState {
        object Idle : State
        object Loading : State
    }

    private sealed class Event : UiEvent {
        object Click : Event()
    }

    private sealed class Effect : UiEffect {
        object Navigate : Effect()
    }

    @Test
    fun `UiState can be implemented by sealed interface`() {
        val state: UiState = State.Idle
        assert(state is State.Idle)
    }

    @Test
    fun `UiEvent can be implemented by sealed class`() {
        val event: UiEvent = Event.Click
        assert(event is Event.Click)
    }

    @Test
    fun `UiEffect can be implemented by sealed class`() {
        val effect: UiEffect = Effect.Navigate
        assert(effect is Effect.Navigate)
    }
}
