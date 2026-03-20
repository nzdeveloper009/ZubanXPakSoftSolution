package com.android.zubanx.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.Locale
import java.util.UUID

/**
 * Singleton wrapper around Android [TextToSpeech].
 *
 * Exposes [state] as a `StateFlow<TtsState>` so any Fragment or ViewModel can observe
 * the current TTS status without polling. Registered as a `single` in [ttsModule].
 *
 * **Lifecycle:** Call [release] when the Application is destroyed (handled by Koin scope teardown).
 *
 * @param context Application context — must be the Application context to avoid leaks.
 */
class TtsManager(context: Context) {

    private val _state = MutableStateFlow<TtsState>(TtsState.Idle)

    /** Current TTS state. Observe via `StateFlow.collect`. */
    val state: StateFlow<TtsState> = _state.asStateFlow()

    @Volatile private var isInitialized = false
    private var pendingSpeak: Pair<String, String>? = null

    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            pendingSpeak?.let { (text, lang) ->
                pendingSpeak = null
                speak(text, lang)
            }
        } else {
            Timber.e("TtsManager: TextToSpeech initialisation failed (status=$status)")
            _state.value = TtsState.Error("TTS initialisation failed")
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                _state.value = TtsState.Idle
            }

            @Deprecated("Deprecated in API 21")
            override fun onError(utteranceId: String?) {
                _state.value = TtsState.Error("TTS utterance failed")
                Timber.w("TtsManager: utterance error for id=$utteranceId")
            }
        })
    }

    /**
     * Speaks [text] using the locale for [languageTag] (BCP-47, e.g. `"en"`, `"es-MX"`).
     *
     * Sets state to [TtsState.Speaking] immediately and returns to [TtsState.Idle] when done.
     * If the engine is already speaking, the current utterance is interrupted.
     * If TTS is not yet initialised, the request is queued and replayed once ready.
     */
    fun speak(text: String, languageTag: String = "en") {
        if (!isInitialized) {
            pendingSpeak = text to languageTag
            return
        }
        tts.language = Locale.forLanguageTag(languageTag)
        _state.value = TtsState.Speaking(text)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    /**
     * Stops any in-progress speech immediately. State returns to [TtsState.Idle].
     */
    fun stop() {
        tts.stop()
        _state.value = TtsState.Idle
    }

    /**
     * Releases TTS resources. After calling this, [speak] and [stop] must not be called.
     */
    fun release() {
        tts.stop()
        tts.shutdown()
    }
}
