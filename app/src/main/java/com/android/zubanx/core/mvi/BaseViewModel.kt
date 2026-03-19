package com.android.zubanx.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseViewModel<S : UiState, E : UiEvent, Ef : UiEffect>(
    initialState: S
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effect = Channel<Ef>(Channel.BUFFERED)
    val effect: Flow<Ef> = _effect.receiveAsFlow()

    protected fun setState(reducer: S.() -> S) {
        _state.update { it.reducer() }
    }

    protected fun sendEffect(effect: Ef) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }

    abstract fun onEvent(event: E)
}
