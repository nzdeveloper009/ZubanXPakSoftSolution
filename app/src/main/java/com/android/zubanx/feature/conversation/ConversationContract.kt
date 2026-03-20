// app/src/main/java/com/android/zubanx/feature/conversation/ConversationContract.kt
package com.android.zubanx.feature.conversation

import com.android.zubanx.core.mvi.UiEffect
import com.android.zubanx.core.mvi.UiEvent
import com.android.zubanx.core.mvi.UiState
import com.android.zubanx.feature.translate.LanguageItem

object ConversationContract {

    enum class SpeakerSide { A, B }

    data class ConversationMessage(
        val id: Long,
        val speakerSide: SpeakerSide,
        val originalText: String,
        val translatedText: String,
        val originalLang: String,
        val targetLang: String,
        val timestamp: Long
    )

    sealed interface State : UiState {
        data class Active(
            val messages: List<ConversationMessage> = emptyList(),
            val langA: LanguageItem = LanguageItem.fromCode("en"),
            val langB: LanguageItem = LanguageItem.fromCode("ur"),
            val isTranslatingA: Boolean = false,
            val isTranslatingB: Boolean = false
        ) : State
    }

    sealed class Event : UiEvent {
        data class MicResultA(val text: String) : Event()
        data class MicResultB(val text: String) : Event()
        data class LangASelected(val lang: LanguageItem) : Event()
        data class LangBSelected(val lang: LanguageItem) : Event()
        data object LaunchMicA : Event()
        data object LaunchMicB : Event()
        data object ClearConversation : Event()
    }

    sealed class Effect : UiEffect {
        data class ShowToast(val message: String) : Effect()
        data class SpeakText(val text: String, val langCode: String) : Effect()
        data class LaunchMic(val langCode: String, val speaker: SpeakerSide) : Effect()
    }
}
