package com.android.zubanx.tts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Singleton wrapper around Android [SpeechRecognizer].
 *
 * Exposes [state] as `StateFlow<SttState>` for reactive observation.
 * Registered as a `single` in [ttsModule].
 *
 * **Threading:** [SpeechRecognizer] must be created and used on the main thread.
 * All public functions must be called from the main thread.
 *
 * @param context Application context.
 */
class SttManager(context: Context) {

    private val _state = MutableStateFlow<SttState>(SttState.Idle)

    /** Current STT state. Observe via `StateFlow.collect`. */
    val state: StateFlow<SttState> = _state.asStateFlow()

    private val recognizer: SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(context)

    init {
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = SttState.Listening
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                val message = sttErrorMessage(error)
                Timber.w("SttManager: recognition error $error — $message")
                _state.value = SttState.Error(message)
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                _state.value = SttState.Result(text)
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    /**
     * Starts listening for speech in the locale for [languageTag] (BCP-47).
     * Must be called from the main thread.
     */
    fun startListening(languageTag: String = "en-US") {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer.startListening(intent)
    }

    /**
     * Stops listening. Final results may still arrive after this call.
     * Must be called from the main thread.
     */
    fun stopListening() {
        recognizer.stopListening()
    }

    /**
     * Releases recogniser resources. Must be called from the main thread.
     * After calling this, [startListening] and [stopListening] must not be called.
     */
    fun release() {
        recognizer.destroy()
    }

    private fun sttErrorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
        SpeechRecognizer.ERROR_NETWORK -> "Network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recogniser busy"
        SpeechRecognizer.ERROR_SERVER -> "Server error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
        else -> "Unknown error ($error)"
    }
}
