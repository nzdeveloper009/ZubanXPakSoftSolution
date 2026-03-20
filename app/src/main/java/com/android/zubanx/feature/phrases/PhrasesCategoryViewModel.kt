package com.android.zubanx.feature.phrases

import androidx.lifecycle.viewModelScope
import com.android.zubanx.core.mvi.BaseViewModel
import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.local.datastore.AppPreferences
import com.android.zubanx.domain.usecase.phrases.TranslatePhraseUseCase
import com.android.zubanx.feature.phrases.data.PhrasesData
import com.android.zubanx.feature.translate.LanguageItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PhrasesCategoryViewModel(
    private val translateUseCase: TranslatePhraseUseCase,
    private val appPreferences: AppPreferences,
    categoryId: String
) : BaseViewModel<PhrasesCategoryContract.State, PhrasesCategoryContract.Event, PhrasesCategoryContract.Effect>(
    run {
        val category = PhrasesData.categoryById(categoryId)
        PhrasesCategoryContract.State.Active(
            category = category,
            displayPhrases = PhrasesData.phrases[category] ?: emptyList()
        )
    }
) {

    init {
        viewModelScope.launch {
            val sourceLangCode = appPreferences.sourceLang.first()
            val targetLangCode = appPreferences.targetLang.first()
            val sourceLang = if (sourceLangCode == "auto") LanguageItem.fromCode("en")
                             else LanguageItem.fromCode(sourceLangCode)
            val targetLang = LanguageItem.fromCode(targetLangCode)
            val current = activeState ?: return@launch
            setState { current.copy(langSource = sourceLang, langTarget = targetLang) }
        }
    }

    override fun onEvent(event: PhrasesCategoryContract.Event) {
        when (event) {
            is PhrasesCategoryContract.Event.ExpandPhrase -> handleExpand(event.index)
            is PhrasesCategoryContract.Event.CollapsePhrase -> collapse()
            is PhrasesCategoryContract.Event.LangSourceSelected -> changeSource(event.lang)
            is PhrasesCategoryContract.Event.LangTargetSelected -> changeTarget(event.lang)
            is PhrasesCategoryContract.Event.SwapLanguages -> swapLanguages()
            is PhrasesCategoryContract.Event.RetryTranslation -> retryTranslation(event.index)
            is PhrasesCategoryContract.Event.SpeakPhrase -> {
                val s = activeState ?: return
                val key = "${s.langSource.code}:${s.langTarget.code}:${event.index}"
                val text = s.translationCache[key] ?: return
                sendEffect(PhrasesCategoryContract.Effect.SpeakText(text, s.langTarget.code))
            }
            is PhrasesCategoryContract.Event.CopyPhrase -> {
                val s = activeState ?: return
                val key = "${s.langSource.code}:${s.langTarget.code}:${event.index}"
                val text = s.translationCache[key] ?: return
                sendEffect(PhrasesCategoryContract.Effect.CopyToClipboard(text))
            }
        }
    }

    private val activeState get() = state.value as? PhrasesCategoryContract.State.Active

    private fun handleExpand(index: Int) {
        val s = activeState ?: return
        if (s.expandedIndex == index) {
            setState { (this as PhrasesCategoryContract.State.Active).copy(expandedIndex = null) }
            return
        }
        setState { (this as PhrasesCategoryContract.State.Active).copy(expandedIndex = index) }
        triggerTranslationIfNeeded(index)
    }

    private fun collapse() {
        setState { (this as PhrasesCategoryContract.State.Active).copy(expandedIndex = null) }
    }

    private fun changeSource(lang: LanguageItem) {
        setState {
            (this as PhrasesCategoryContract.State.Active).copy(
                langSource = lang,
                expandedIndex = null
            )
        }
        viewModelScope.launch { appPreferences.setSourceLang(lang.code) }
        refreshDisplayPhrases()
    }

    private fun changeTarget(lang: LanguageItem) {
        setState {
            (this as PhrasesCategoryContract.State.Active).copy(
                langTarget = lang,
                expandedIndex = null
            )
        }
        viewModelScope.launch { appPreferences.setTargetLang(lang.code) }
    }

    private fun swapLanguages() {
        val s = activeState ?: return
        setState {
            (this as PhrasesCategoryContract.State.Active).copy(
                langSource = s.langTarget,
                langTarget = s.langSource,
                expandedIndex = null
            )
        }
        viewModelScope.launch {
            appPreferences.setSourceLang(s.langTarget.code)
            appPreferences.setTargetLang(s.langSource.code)
        }
        refreshDisplayPhrases()
    }

    // Fix 2: Only translate phrases when source language is non-English.
    // Re-reads activeState after translateUseCase returns and discards the result
    // if the language pair has changed since the request was dispatched.
    private fun refreshDisplayPhrases() {
        val s = activeState ?: return
        val basePhrases = PhrasesData.phrases[s.category] ?: return
        if (s.langSource.code == "en") {
            setState { (this as PhrasesCategoryContract.State.Active).copy(displayPhrases = basePhrases) }
            return
        }
        val sourceLangAtDispatch = s.langSource.code
        basePhrases.forEachIndexed { index, phrase ->
            val cacheKey = "en:${sourceLangAtDispatch}:$index"
            if (s.translationCache.containsKey(cacheKey)) {
                val translated = s.translationCache[cacheKey]!!
                setState {
                    val active = this as PhrasesCategoryContract.State.Active
                    val updatedDisplay = active.displayPhrases.toMutableList().also {
                        if (index < it.size) it[index] = translated
                    }
                    active.copy(displayPhrases = updatedDisplay)
                }
            } else {
                viewModelScope.launch {
                    val result = try {
                        translateUseCase(phrase, "en", sourceLangAtDispatch)
                    } catch (e: Exception) {
                        return@launch  // keep English fallback on unexpected errors
                    }
                    // Re-read state after suspend; discard if language has changed
                    val current = activeState ?: return@launch
                    if (current.langSource.code != sourceLangAtDispatch) return@launch
                    when (result) {
                        is NetworkResult.Success -> {
                            val translated = result.data.translatedText
                            setState {
                                val active = this as PhrasesCategoryContract.State.Active
                                val updatedDisplay = active.displayPhrases.toMutableList().also {
                                    if (index < it.size) it[index] = translated
                                }
                                active.copy(
                                    displayPhrases = updatedDisplay,
                                    translationCache = active.translationCache + (cacheKey to translated)
                                )
                            }
                        }
                        is NetworkResult.Error -> { /* keep English fallback */ }
                    }
                }
            }
        }
    }

    // Fix 1: Re-reads activeState after translateUseCase returns and discards the result
    // if the language pair has changed since the request was dispatched.
    // Fix 3: errorIndices is cleared here (merged with loadingIndices update) so the error
    // is only removed when we're actually about to launch a request.
    private fun triggerTranslationIfNeeded(index: Int) {
        val s = activeState ?: return
        val cacheKey = "${s.langSource.code}:${s.langTarget.code}:$index"
        if (s.translationCache.containsKey(cacheKey)) return
        if (s.loadingIndices.contains(index)) return

        val phraseToTranslate = s.displayPhrases.getOrNull(index) ?: return

        setState {
            (this as PhrasesCategoryContract.State.Active).copy(
                loadingIndices = loadingIndices + index,
                errorIndices = errorIndices - index  // clear error only when actually launching
            )
        }

        viewModelScope.launch {
            val result = try {
                translateUseCase(phraseToTranslate, s.langSource.code, s.langTarget.code)
            } catch (e: Exception) {
                setState {
                    (this as PhrasesCategoryContract.State.Active).copy(
                        loadingIndices = loadingIndices - index,
                        errorIndices = errorIndices + index
                    )
                }
                return@launch
            }
            // Re-read state after suspend; discard if language pair has changed
            val current = activeState ?: return@launch
            val currentKey = "${current.langSource.code}:${current.langTarget.code}:$index"
            if (currentKey != cacheKey) return@launch  // language changed while in-flight, discard
            when (result) {
                is NetworkResult.Success -> {
                    val translated = result.data.translatedText
                    setState {
                        (this as PhrasesCategoryContract.State.Active).copy(
                            translationCache = translationCache + (currentKey to translated),
                            loadingIndices = loadingIndices - index
                        )
                    }
                }
                is NetworkResult.Error -> {
                    setState {
                        (this as PhrasesCategoryContract.State.Active).copy(
                            loadingIndices = loadingIndices - index,
                            errorIndices = errorIndices + index
                        )
                    }
                }
            }
        }
    }

    // Fix 3: retryTranslation no longer pre-clears errorIndices; that is done inside
    // triggerTranslationIfNeeded when the request is actually launched.
    private fun retryTranslation(index: Int) {
        triggerTranslationIfNeeded(index)
    }

    // NavigateToZoom is not sent by the ViewModel — zoom navigation is handled directly
    // in PhrasesCategoryFragment via the adapter's onZoom lambda.
}
