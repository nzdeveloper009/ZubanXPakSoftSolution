// app/src/main/java/com/android/zubanx/feature/conversation/ConversationViewModel.kt
package com.android.zubanx.feature.conversation

import androidx.lifecycle.viewModelScope
import com.android.zubanx.core.mvi.BaseViewModel
import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.domain.usecase.conversation.ConversationTranslateUseCase
import com.android.zubanx.feature.translate.LanguageItem
import kotlinx.coroutines.launch

class ConversationViewModel(
    private val translateUseCase: ConversationTranslateUseCase
) : BaseViewModel<ConversationContract.State, ConversationContract.Event, ConversationContract.Effect>(
    ConversationContract.State.Active()
) {

    private var nextId = 0L

    override fun onEvent(event: ConversationContract.Event) {
        when (event) {
            is ConversationContract.Event.LaunchMicA -> {
                val state = activeState ?: return
                sendEffect(ConversationContract.Effect.LaunchMic(state.langA.code, ConversationContract.SpeakerSide.A))
            }
            is ConversationContract.Event.LaunchMicB -> {
                val state = activeState ?: return
                sendEffect(ConversationContract.Effect.LaunchMic(state.langB.code, ConversationContract.SpeakerSide.B))
            }
            is ConversationContract.Event.MicResultA -> translateTurn(event.text, ConversationContract.SpeakerSide.A)
            is ConversationContract.Event.MicResultB -> translateTurn(event.text, ConversationContract.SpeakerSide.B)
            is ConversationContract.Event.LangASelected -> setState { (this as ConversationContract.State.Active).copy(langA = event.lang) }
            is ConversationContract.Event.LangBSelected -> setState { (this as ConversationContract.State.Active).copy(langB = event.lang) }
            is ConversationContract.Event.ClearConversation -> setState { (this as ConversationContract.State.Active).copy(messages = emptyList()) }
        }
    }

    private val activeState: ConversationContract.State.Active?
        get() = state.value as? ConversationContract.State.Active

    private fun translateTurn(text: String, speaker: ConversationContract.SpeakerSide) {
        val currentState = activeState ?: return
        val (sourceLang, targetLang) = when (speaker) {
            ConversationContract.SpeakerSide.A -> currentState.langA.code to currentState.langB.code
            ConversationContract.SpeakerSide.B -> currentState.langB.code to currentState.langA.code
        }

        setState {
            (this as ConversationContract.State.Active).copy(
                isTranslatingA = speaker == ConversationContract.SpeakerSide.A,
                isTranslatingB = speaker == ConversationContract.SpeakerSide.B
            )
        }

        viewModelScope.launch {
            when (val result = translateUseCase(text, sourceLang, targetLang)) {
                is NetworkResult.Success -> {
                    val message = ConversationContract.ConversationMessage(
                        id = nextId++,
                        speakerSide = speaker,
                        originalText = text,
                        translatedText = result.data.translatedText,
                        originalLang = sourceLang,
                        targetLang = targetLang,
                        timestamp = System.currentTimeMillis()
                    )
                    setState {
                        (this as ConversationContract.State.Active).copy(
                            messages = messages + message,
                            isTranslatingA = false,
                            isTranslatingB = false
                        )
                    }
                    sendEffect(ConversationContract.Effect.SpeakText(result.data.translatedText, targetLang))
                }
                is NetworkResult.Error -> {
                    setState {
                        (this as ConversationContract.State.Active).copy(
                            isTranslatingA = false,
                            isTranslatingB = false
                        )
                    }
                    sendEffect(ConversationContract.Effect.ShowToast(result.message))
                }
            }
        }
    }
}
