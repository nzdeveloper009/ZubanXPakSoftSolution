package com.android.zubanx.tts

/**
 * UI-observable state for the Text-to-Speech engine.
 *
 * Collected via [TtsManager.state] — a `StateFlow<TtsState>` registered as
 * a `single` in [ttsModule].
 */
sealed interface TtsState {
    /** Engine is initialised and idle. */
    data object Idle : TtsState

    /** Engine is currently speaking [text]. */
    data class Speaking(val text: String) : TtsState

    /** Engine encountered an error described by [message]. */
    data class Error(val message: String) : TtsState
}
