package com.android.zubanx.feature.dictionary

import androidx.lifecycle.viewModelScope
import com.android.zubanx.core.mvi.BaseViewModel
import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.domain.model.DictionaryEntry
import com.android.zubanx.domain.usecase.dictionary.EnrichWithAiUseCase
import kotlinx.coroutines.launch

class WordDetailViewModel(
    private val enrichWithAiUseCase: EnrichWithAiUseCase
) : BaseViewModel<WordDetailContract.State, WordDetailContract.Event, WordDetailContract.Effect>(
    WordDetailContract.State.Loaded(DictionaryEntry(word = "", language = "", definition = "", timestamp = 0L))
) {

    override fun onEvent(event: WordDetailContract.Event) {
        when (event) {
            is WordDetailContract.Event.Load -> setState {
                WordDetailContract.State.Loaded(entry = event.entry)
            }
            is WordDetailContract.Event.EnrichWithAi -> enrichWithAi(event.expert)
            is WordDetailContract.Event.SpeakWord -> speakWord()
            is WordDetailContract.Event.CopyDefinition -> copyDefinition()
        }
    }

    private fun enrichWithAi(expert: String) {
        val loaded = state.value as? WordDetailContract.State.Loaded ?: return
        setState { (loaded as WordDetailContract.State.Loaded).copy(aiLoading = true) }
        viewModelScope.launch {
            val entry = (state.value as? WordDetailContract.State.Loaded)?.entry ?: return@launch
            when (val result = enrichWithAiUseCase(entry.word, entry.language, expert)) {
                is NetworkResult.Success -> setState {
                    (state.value as? WordDetailContract.State.Loaded)?.copy(
                        aiInsight = result.data,
                        aiLoading = false
                    ) ?: this
                }
                is NetworkResult.Error -> {
                    setState {
                        (state.value as? WordDetailContract.State.Loaded)?.copy(aiLoading = false) ?: this
                    }
                    sendEffect(WordDetailContract.Effect.ShowToast(result.message))
                }
            }
        }
    }

    private fun speakWord() {
        val entry = (state.value as? WordDetailContract.State.Loaded)?.entry ?: return
        sendEffect(WordDetailContract.Effect.SpeakText(entry.word, entry.language))
    }

    private fun copyDefinition() {
        val entry = (state.value as? WordDetailContract.State.Loaded)?.entry ?: return
        sendEffect(WordDetailContract.Effect.CopyToClipboard(entry.definition))
        sendEffect(WordDetailContract.Effect.ShowToast("Definition copied"))
    }
}
