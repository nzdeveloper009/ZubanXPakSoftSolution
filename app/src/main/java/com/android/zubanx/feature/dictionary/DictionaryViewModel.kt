package com.android.zubanx.feature.dictionary

import androidx.lifecycle.viewModelScope
import com.android.zubanx.core.mvi.BaseViewModel
import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.domain.model.DictionaryEntry
import com.android.zubanx.domain.usecase.dictionary.GetDictionaryHistoryUseCase
import com.android.zubanx.domain.usecase.dictionary.LookupWordUseCase
import kotlinx.coroutines.launch

class DictionaryViewModel(
    private val lookupUseCase: LookupWordUseCase,
    private val historyUseCase: GetDictionaryHistoryUseCase
) : BaseViewModel<DictionaryContract.State, DictionaryContract.Event, DictionaryContract.Effect>(
    DictionaryContract.State.Idle
) {
    private var currentQuery = ""
    private var historyList: List<DictionaryEntry> = emptyList()

    init {
        viewModelScope.launch {
            historyUseCase().collect { list ->
                historyList = list
                val current = state.value
                if (current is DictionaryContract.State.Success) {
                    setState { (current as DictionaryContract.State.Success).copy(history = list) }
                } else if (current is DictionaryContract.State.Error) {
                    setState { (current as DictionaryContract.State.Error).copy(history = list) }
                }
            }
        }
    }

    override fun onEvent(event: DictionaryContract.Event) {
        when (event) {
            is DictionaryContract.Event.QueryChanged -> currentQuery = event.text
            is DictionaryContract.Event.SearchClicked -> search(currentQuery)
            is DictionaryContract.Event.MicResult -> {
                currentQuery = event.text
                search(event.text)
            }
            is DictionaryContract.Event.ClearSearch -> {
                currentQuery = ""
                setState { DictionaryContract.State.Idle }
            }
            is DictionaryContract.Event.HistoryItemClicked -> loadFromHistory(event.entry)
            is DictionaryContract.Event.NavigateToDetail -> {
                val entry = (state.value as? DictionaryContract.State.Success)?.entry ?: return
                sendEffect(DictionaryContract.Effect.OpenWordDetail(entry))
            }
        }
    }

    private fun search(word: String, language: String = "en") {
        if (word.isBlank()) {
            setState { DictionaryContract.State.Error("Enter a word to search", history = historyList) }
            return
        }
        setState { DictionaryContract.State.Searching }
        viewModelScope.launch {
            when (val result = lookupUseCase(word, language)) {
                is NetworkResult.Success -> setState {
                    DictionaryContract.State.Success(entry = result.data, history = historyList)
                }
                is NetworkResult.Error -> setState {
                    DictionaryContract.State.Error(message = result.message, history = historyList)
                }
            }
        }
    }

    private fun loadFromHistory(entry: DictionaryEntry) {
        currentQuery = entry.word
        setState { DictionaryContract.State.Success(entry = entry, history = historyList) }
    }
}
