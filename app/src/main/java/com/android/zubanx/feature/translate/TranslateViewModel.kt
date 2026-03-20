package com.android.zubanx.feature.translate

import androidx.lifecycle.viewModelScope
import com.android.zubanx.core.mvi.BaseViewModel
import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.local.datastore.AppPreferences
import com.android.zubanx.domain.model.Translation
import com.android.zubanx.domain.usecase.translate.AddFavouriteFromTranslationUseCase
import com.android.zubanx.domain.usecase.translate.DeleteTranslationUseCase
import com.android.zubanx.domain.usecase.translate.GetTranslationHistoryUseCase
import com.android.zubanx.domain.usecase.translate.TranslateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TranslateViewModel(
    private val translateUseCase: TranslateUseCase,
    private val historyUseCase: GetTranslationHistoryUseCase,
    private val deleteUseCase: DeleteTranslationUseCase,
    private val addFavouriteUseCase: AddFavouriteFromTranslationUseCase,
    private val appPreferences: AppPreferences
) : BaseViewModel<TranslateContract.State, TranslateContract.Event, TranslateContract.Effect>(
    TranslateContract.State.Idle
) {

    private val inputText = MutableStateFlow("")
    private var currentSourceLang = LanguageItem.DETECT
    private var currentTargetLang = LanguageItem.fromCode("en")
    private var currentExpert = "DEFAULT"
    private var lastSuccessTranslation: Translation? = null
    private var historyList: List<Translation> = emptyList()

    init {
        viewModelScope.launch {
            currentExpert = appPreferences.selectedExpert.first()
            val sourceLangCode = appPreferences.sourceLang.first()
            val targetLangCode = appPreferences.targetLang.first()
            currentSourceLang = if (sourceLangCode == "auto") LanguageItem.DETECT
            else LanguageItem.fromCode(sourceLangCode)
            currentTargetLang = LanguageItem.fromCode(targetLangCode)
        }
        viewModelScope.launch {
            historyUseCase().collect { list ->
                historyList = list
                val current = state.value
                if (current is TranslateContract.State.Success) {
                    setState { (current as TranslateContract.State.Success).copy(history = list) }
                } else if (current is TranslateContract.State.Error) {
                    setState { (current as TranslateContract.State.Error).copy(history = list) }
                }
            }
        }
        viewModelScope.launch {
            appPreferences.selectedExpert.collect { expert -> currentExpert = expert }
        }
    }

    override fun onEvent(event: TranslateContract.Event) {
        when (event) {
            is TranslateContract.Event.InputChanged -> inputText.value = event.text
            is TranslateContract.Event.TranslateClicked -> translate(inputText.value)
            is TranslateContract.Event.MicResult -> {
                inputText.value = event.text
                translate(event.text)
            }
            is TranslateContract.Event.SourceLangSelected -> currentSourceLang = event.language
            is TranslateContract.Event.TargetLangSelected -> currentTargetLang = event.language
            is TranslateContract.Event.SwapLanguages -> swapLanguages()
            is TranslateContract.Event.ClearInput -> {
                inputText.value = ""
                setState { TranslateContract.State.Idle }
            }
            is TranslateContract.Event.CopyTranslation -> copyTranslation()
            is TranslateContract.Event.SpeakTranslation -> speakTranslation()
            is TranslateContract.Event.AddToFavourites -> addToFavourites()
            is TranslateContract.Event.ShareTranslation -> shareTranslation()
            is TranslateContract.Event.HistoryItemClicked -> loadFromHistory(event.translation)
            is TranslateContract.Event.DeleteHistoryItem -> deleteHistory(event.id)
        }
    }

    private fun translate(text: String) {
        if (text.isBlank()) {
            setState { TranslateContract.State.Error("Enter text to translate", history = historyList) }
            return
        }
        setState { TranslateContract.State.Translating }
        viewModelScope.launch {
            when (val result = translateUseCase(text, currentSourceLang.code, currentTargetLang.code, currentExpert)) {
                is NetworkResult.Success -> {
                    val translation = Translation(
                        sourceText = text,
                        translatedText = result.data.translatedText,
                        sourceLang = currentSourceLang.code,
                        targetLang = currentTargetLang.code,
                        expert = currentExpert,
                        timestamp = System.currentTimeMillis()
                    )
                    lastSuccessTranslation = translation
                    setState {
                        TranslateContract.State.Success(
                            inputText = text,
                            translatedText = result.data.translatedText,
                            sourceLang = currentSourceLang,
                            targetLang = currentTargetLang,
                            expert = currentExpert,
                            history = historyList
                        )
                    }
                }
                is NetworkResult.Error -> {
                    setState {
                        TranslateContract.State.Error(
                            message = result.message,
                            inputText = text,
                            history = historyList
                        )
                    }
                }
            }
        }
    }

    private fun swapLanguages() {
        if (currentSourceLang == LanguageItem.DETECT) return
        val temp = currentSourceLang
        currentSourceLang = currentTargetLang
        currentTargetLang = temp
        val text = inputText.value
        if (text.isNotBlank()) translate(text)
    }

    private fun copyTranslation() {
        val translated = (state.value as? TranslateContract.State.Success)?.translatedText ?: return
        sendEffect(TranslateContract.Effect.CopyToClipboard(translated))
        sendEffect(TranslateContract.Effect.ShowToast("Copied to clipboard"))
    }

    private fun speakTranslation() {
        val success = state.value as? TranslateContract.State.Success ?: return
        sendEffect(TranslateContract.Effect.SpeakText(success.translatedText, success.targetLang.code))
    }

    private fun addToFavourites() {
        val translation = lastSuccessTranslation ?: return
        viewModelScope.launch {
            addFavouriteUseCase(translation)
            sendEffect(TranslateContract.Effect.ShowToast("Added to favourites"))
        }
    }

    private fun shareTranslation() {
        val success = state.value as? TranslateContract.State.Success ?: return
        sendEffect(TranslateContract.Effect.ShareText(success.translatedText))
    }

    private fun loadFromHistory(translation: Translation) {
        inputText.value = translation.sourceText
        currentSourceLang = LanguageItem.fromCode(translation.sourceLang)
        currentTargetLang = LanguageItem.fromCode(translation.targetLang)
        currentExpert = translation.expert
        lastSuccessTranslation = translation
        setState {
            TranslateContract.State.Success(
                inputText = translation.sourceText,
                translatedText = translation.translatedText,
                sourceLang = currentSourceLang,
                targetLang = currentTargetLang,
                expert = translation.expert,
                history = historyList
            )
        }
    }

    private fun deleteHistory(id: Long) {
        viewModelScope.launch { deleteUseCase(id) }
    }
}
