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
import timber.log.Timber

/**
 * Base class for all feature ViewModels in the MVI architecture.
 *
 * @param S Screen state type — must implement [UiState].
 * @param E User-intent event type — must implement [UiEvent].
 * @param Ef One-shot side-effect type — must implement [UiEffect].
 * @param initialState The state the screen starts in.
 */
abstract class BaseViewModel<S : UiState, E : UiEvent, Ef : UiEffect>(
    initialState: S
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)

    /** The current screen state, never null, replayed to new collectors. */
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effect = Channel<Ef>(Channel.BUFFERED)

    /**
     * One-shot side effects such as navigation or showing a Toast.
     *
     * IMPORTANT: This is a unicast [Channel]-backed flow. Only one collector
     * should subscribe at a time. Collect inside [repeatOnLifecycle] via
     * [collectFlow] to ensure the subscription is torn down on Stop.
     */
    val effect: Flow<Ef> = _effect.receiveAsFlow()

    protected fun setState(reducer: S.() -> S) {
        _state.update { it.reducer() }
    }

    protected fun sendEffect(effect: Ef) {
        viewModelScope.launch {
            val sent = _effect.trySend(effect)
            if (!sent.isSuccess) {
                Timber.w("Effect dropped (channel full or closed): $effect")
            }
        }
    }

    abstract fun onEvent(event: E)
}
