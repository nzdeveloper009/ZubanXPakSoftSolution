package com.android.zubanx.feature.translate

import androidx.lifecycle.viewModelScope
import com.android.zubanx.core.mvi.BaseViewModel
import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.local.datastore.AppPreferences
import com.android.zubanx.domain.model.AiTone
import com.android.zubanx.domain.model.FavouriteCategory
import com.android.zubanx.domain.model.Translation
import com.android.zubanx.domain.usecase.favourite.DeleteFavouriteUseCase
import com.android.zubanx.domain.usecase.favourite.GetFavouritesByCategoryUseCase
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
    private val getFavouritesUseCase: GetFavouritesByCategoryUseCase,
    private val deleteFavouriteUseCase: DeleteFavouriteUseCase,
    private val appPreferences: AppPreferences
) : BaseViewModel<TranslateContract.State, TranslateContract.Event, TranslateContract.Effect>(
    TranslateContract.State.Idle()
) {

    private val inputText = MutableStateFlow("")
    private var currentSourceLang = LanguageItem.DETECT
    private var currentTargetLang = LanguageItem.fromCode("en")
    private var currentExpert = "DEFAULT"
    private var currentAiTone = AiTone.ORIGINAL.key
    private var lastSuccessTranslation: Translation? = null
    private var historyList: List<Translation> = emptyList()
    // key = "${sourceText}|${targetLang}", value = favourite row id
    private var favouriteKeyToId: Map<String, Long> = emptyMap()

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
                when (current) {
                    is TranslateContract.State.Idle -> setState { current.copy(history = list) }
                    is TranslateContract.State.Success -> setState { current.copy(history = list) }
                    is TranslateContract.State.Error -> setState { current.copy(history = list) }
                    else -> {}
                }
            }
        }
        viewModelScope.launch {
            getFavouritesUseCase(FavouriteCategory.TRANSLATE).collect { favourites ->
                favouriteKeyToId = favourites.associate { fav ->
                    "${fav.sourceText}|${fav.targetLang}" to fav.id
                }
                val keys = favouriteKeyToId.keys
                val current = state.value
                when (current) {
                    is TranslateContract.State.Idle -> setState { current.copy(favouritedKeys = keys) }
                    is TranslateContract.State.Success -> setState { current.copy(favouritedKeys = keys) }
                    is TranslateContract.State.Error -> setState { current.copy(favouritedKeys = keys) }
                    else -> {}
                }
            }
        }
        viewModelScope.launch {
            appPreferences.selectedExpert.collect { expert -> currentExpert = expert }
        }
        viewModelScope.launch {
            appPreferences.aiTone.collect { tone -> currentAiTone = tone }
        }
    }

    override fun onEvent(event: TranslateContract.Event) {
        when (event) {
            is TranslateContract.Event.InputChanged -> inputText.value = event.text
            is TranslateContract.Event.TranslateClicked -> translate(inputText.value)
            is TranslateContract.Event.MicResult -> {
                inputText.value = event.text
                sendEffect(TranslateContract.Effect.SetInputText(event.text))
            }
            is TranslateContract.Event.SourceLangSelected -> {
                currentSourceLang = event.language
                viewModelScope.launch { appPreferences.setSourceLang(event.language.code) }
            }
            is TranslateContract.Event.TargetLangSelected -> {
                currentTargetLang = event.language
                viewModelScope.launch { appPreferences.setTargetLang(event.language.code) }
            }
            is TranslateContract.Event.SwapLanguages -> swapLanguages()
            is TranslateContract.Event.ClearInput -> {
                inputText.value = ""
                setState { TranslateContract.State.Idle(history = historyList, favouritedKeys = favouriteKeyToId.keys) }
            }
            is TranslateContract.Event.CopyTranslation -> copyTranslation()
            is TranslateContract.Event.SpeakTranslation -> speakTranslation()
            is TranslateContract.Event.AddToFavourites -> toggleFavourite()
            is TranslateContract.Event.ShareTranslation -> shareTranslation()
            is TranslateContract.Event.HistoryItemClicked -> loadFromHistory(event.translation)
            is TranslateContract.Event.DeleteHistoryItem -> deleteHistory(event.id)
            is TranslateContract.Event.SpeakHistoryItem -> speakHistoryItem(event.translation)
            is TranslateContract.Event.ToggleHistoryFavourite -> toggleHistoryFavourite(event.translation)
            is TranslateContract.Event.ShareHistoryItem -> shareHistoryItem(event.translation)
            is TranslateContract.Event.CopyHistoryItem -> copyHistoryItem(event.translation)
        }
    }

    private fun translate(text: String) {
        if (text.isBlank()) {
            setState {
                TranslateContract.State.Error(
                    "Enter text to translate",
                    history = historyList,
                    favouritedKeys = favouriteKeyToId.keys
                )
            }
            return
        }
        setState { TranslateContract.State.Translating }
        viewModelScope.launch {
            when (val result = translateUseCase(text, currentSourceLang.code, currentTargetLang.code, currentExpert, currentAiTone)) {
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
                            history = historyList,
                            favouritedKeys = favouriteKeyToId.keys
                        )
                    }
                }
                is NetworkResult.Error -> {
                    setState {
                        TranslateContract.State.Error(
                            message = result.message,
                            inputText = text,
                            history = historyList,
                            favouritedKeys = favouriteKeyToId.keys
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
        sendEffect(TranslateContract.Effect.UpdateLanguages(currentSourceLang, currentTargetLang))
        viewModelScope.launch {
            appPreferences.setSourceLang(currentSourceLang.code)
            appPreferences.setTargetLang(currentTargetLang.code)
        }
        val current = state.value
        if (current is TranslateContract.State.Success) {
            setState { current.copy(sourceLang = currentSourceLang, targetLang = currentTargetLang) }
        }
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

    private fun toggleFavourite() {
        val translation = lastSuccessTranslation ?: return
        val key = "${translation.sourceText}|${translation.targetLang}"
        viewModelScope.launch {
            val existingId = favouriteKeyToId[key]
            if (existingId != null) {
                deleteFavouriteUseCase(existingId)
                sendEffect(TranslateContract.Effect.ShowToast("Removed from favourites"))
            } else {
                addFavouriteUseCase(translation)
                sendEffect(TranslateContract.Effect.ShowToast("Added to favourites"))
            }
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
                history = historyList,
                favouritedKeys = favouriteKeyToId.keys
            )
        }
    }

    private fun deleteHistory(id: Long) {
        viewModelScope.launch { deleteUseCase(id) }
    }

    private fun speakHistoryItem(translation: Translation) {
        sendEffect(TranslateContract.Effect.SpeakText(translation.translatedText, translation.targetLang))
    }

    private fun toggleHistoryFavourite(translation: Translation) {
        val key = "${translation.sourceText}|${translation.targetLang}"
        viewModelScope.launch {
            val existingId = favouriteKeyToId[key]
            if (existingId != null) {
                deleteFavouriteUseCase(existingId)
                sendEffect(TranslateContract.Effect.ShowToast("Removed from favourites"))
            } else {
                addFavouriteUseCase(translation)
                sendEffect(TranslateContract.Effect.ShowToast("Added to favourites"))
            }
        }
    }

    private fun shareHistoryItem(translation: Translation) {
        sendEffect(TranslateContract.Effect.ShareText("${translation.sourceText}\n${translation.translatedText}"))
    }

    private fun copyHistoryItem(translation: Translation) {
        sendEffect(TranslateContract.Effect.CopyToClipboard(translation.translatedText))
        sendEffect(TranslateContract.Effect.ShowToast("Copied to clipboard"))
    }
}
