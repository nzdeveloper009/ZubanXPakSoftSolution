package com.android.zubanx.feature.idioms

import androidx.lifecycle.viewModelScope
import com.android.zubanx.core.mvi.BaseViewModel
import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.local.datastore.AppPreferences
import com.android.zubanx.domain.usecase.phrases.TranslatePhraseUseCase
import com.android.zubanx.feature.idioms.data.IdiomsData
import com.android.zubanx.feature.translate.LanguageItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class IdiomsCategoryViewModel(
    private val translateUseCase: TranslatePhraseUseCase,
    private val appPreferences: AppPreferences,
    categoryId: String
) : BaseViewModel<IdiomsCategoryContract.State, IdiomsCategoryContract.Event, IdiomsCategoryContract.Effect>(
    run {
        val category = IdiomsData.categoryById(categoryId)
        IdiomsCategoryContract.State.Active(
            category = category,
            idioms = IdiomsData.idiomsByCategory(category)
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

    override fun onEvent(event: IdiomsCategoryContract.Event) {
        when (event) {
            is IdiomsCategoryContract.Event.ExpandIdiom        -> handleExpand(event.index)
            is IdiomsCategoryContract.Event.CollapseIdiom      -> collapse()
            is IdiomsCategoryContract.Event.LangSourceSelected -> changeSource(event.lang)
            is IdiomsCategoryContract.Event.LangTargetSelected -> changeTarget(event.lang)
            is IdiomsCategoryContract.Event.SwapLanguages      -> swapLanguages()
            is IdiomsCategoryContract.Event.RetryTranslation   -> triggerTranslation(event.index)
            is IdiomsCategoryContract.Event.SpeakIdiom         -> speakIdiom(event.index)
            is IdiomsCategoryContract.Event.CopyIdiom          -> copyIdiom(event.index)
        }
    }

    private val activeState get() = state.value as? IdiomsCategoryContract.State.Active

    private fun handleExpand(index: Int) {
        val s = activeState ?: return
        if (s.expandedIndex == index) {
            setState { (this as IdiomsCategoryContract.State.Active).copy(expandedIndex = null) }
            return
        }
        setState { (this as IdiomsCategoryContract.State.Active).copy(expandedIndex = index) }
        triggerTranslation(index)
    }

    private fun collapse() {
        setState { (this as IdiomsCategoryContract.State.Active).copy(expandedIndex = null) }
    }

    private fun changeSource(lang: LanguageItem) {
        setState {
            (this as IdiomsCategoryContract.State.Active).copy(langSource = lang, expandedIndex = null)
        }
        viewModelScope.launch { appPreferences.setSourceLang(lang.code) }
    }

    private fun changeTarget(lang: LanguageItem) {
        setState {
            (this as IdiomsCategoryContract.State.Active).copy(langTarget = lang, expandedIndex = null)
        }
        viewModelScope.launch { appPreferences.setTargetLang(lang.code) }
    }

    private fun swapLanguages() {
        val s = activeState ?: return
        setState {
            (this as IdiomsCategoryContract.State.Active).copy(
                langSource = s.langTarget,
                langTarget = s.langSource,
                expandedIndex = null
            )
        }
        viewModelScope.launch {
            appPreferences.setSourceLang(s.langTarget.code)
            appPreferences.setTargetLang(s.langSource.code)
        }
    }

    // Translates entry.meaning; keeps entry.example always in English.
    private fun triggerTranslation(index: Int) {
        val s = activeState ?: return
        // If target == English, no translation needed
        if (s.langTarget.code == "en") return

        val cacheKey = "${s.langSource.code}:${s.langTarget.code}:$index"
        if (s.translationCache.containsKey(cacheKey)) return
        if (s.loadingIndices.contains(index)) return

        val entry = s.idioms.getOrNull(index) ?: return
        val textToTranslate = entry.meaning

        setState {
            (this as IdiomsCategoryContract.State.Active).copy(
                loadingIndices = loadingIndices + index,
                errorIndices = errorIndices - index
            )
        }

        viewModelScope.launch {
            val result = try {
                translateUseCase(textToTranslate, "en", s.langTarget.code)
            } catch (e: Exception) {
                setState {
                    (this as IdiomsCategoryContract.State.Active).copy(
                        loadingIndices = loadingIndices - index,
                        errorIndices = errorIndices + index
                    )
                }
                return@launch
            }
            // Guard: discard if language changed while in-flight
            val current = activeState ?: return@launch
            val currentKey = "${current.langSource.code}:${current.langTarget.code}:$index"
            if (currentKey != cacheKey) return@launch

            when (result) {
                is NetworkResult.Success -> setState {
                    (this as IdiomsCategoryContract.State.Active).copy(
                        translationCache = translationCache + (cacheKey to result.data.translatedText),
                        loadingIndices = loadingIndices - index
                    )
                }
                is NetworkResult.Error -> setState {
                    (this as IdiomsCategoryContract.State.Active).copy(
                        loadingIndices = loadingIndices - index,
                        errorIndices = errorIndices + index
                    )
                }
            }
        }
    }

    private fun speakIdiom(index: Int) {
        val s = activeState ?: return
        val cacheKey = "${s.langSource.code}:${s.langTarget.code}:$index"
        val text = s.translationCache[cacheKey] ?: s.idioms.getOrNull(index)?.meaning ?: return
        val langCode = if (s.translationCache.containsKey(cacheKey)) s.langTarget.code else "en"
        sendEffect(IdiomsCategoryContract.Effect.SpeakText(text, langCode))
    }

    private fun copyIdiom(index: Int) {
        val s = activeState ?: return
        val cacheKey = "${s.langSource.code}:${s.langTarget.code}:$index"
        val entry = s.idioms.getOrNull(index) ?: return
        val meaning = s.translationCache[cacheKey] ?: entry.meaning
        val text = "${entry.title}\n$meaning\n${entry.example}"
        sendEffect(IdiomsCategoryContract.Effect.CopyToClipboard(text))
    }
}
