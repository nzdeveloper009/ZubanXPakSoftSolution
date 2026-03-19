package com.android.zubanx.tts

/**
 * UI-observable state for the Speech-to-Text engine.
 *
 * Collected via [SttManager.state] — a `StateFlow<SttState>` registered as
 * a `single` in [ttsModule].
 */
sealed interface SttState {
    /** Recogniser is ready but not actively listening. */
    data object Idle : SttState

    /** Recogniser is actively recording audio. */
    data object Listening : SttState

    /** Recogniser produced a final transcript. */
    data class Result(val text: String) : SttState

    /** Recogniser encountered an error described by [message]. */
    data class Error(val message: String) : SttState
}
