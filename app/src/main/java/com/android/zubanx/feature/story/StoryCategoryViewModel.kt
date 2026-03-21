package com.android.zubanx.feature.story

import androidx.lifecycle.viewModelScope
import com.android.zubanx.core.mvi.BaseViewModel
import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.local.datastore.AppPreferences
import com.android.zubanx.domain.usecase.phrases.TranslatePhraseUseCase
import com.android.zubanx.feature.story.data.StoryData
import com.android.zubanx.feature.translate.LanguageItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StoryCategoryViewModel(
    private val translateUseCase: TranslatePhraseUseCase,
    private val appPreferences: AppPreferences,
    categoryId: String
) : BaseViewModel<StoryCategoryContract.State, StoryCategoryContract.Event, StoryCategoryContract.Effect>(
    run {
        val category = StoryData.categoryById(categoryId)
        StoryCategoryContract.State.Active(
            category = category,
            stories = StoryData.storiesByCategory(category)
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

    override fun onEvent(event: StoryCategoryContract.Event) {
        when (event) {
            is StoryCategoryContract.Event.ExpandStory        -> expand(event.index)
            is StoryCategoryContract.Event.CollapseStory      -> collapse()
            is StoryCategoryContract.Event.LangSourceSelected -> changeSource(event.lang)
            is StoryCategoryContract.Event.LangTargetSelected -> changeTarget(event.lang)
            is StoryCategoryContract.Event.SwapLanguages      -> swapLanguages()
            is StoryCategoryContract.Event.TranslateStory     -> triggerTranslation(event.index)
            is StoryCategoryContract.Event.RetryTranslation   -> triggerTranslation(event.index)
            is StoryCategoryContract.Event.SpeakStory         -> speakStory(event.index)
            is StoryCategoryContract.Event.CopyStory          -> copyStory(event.index)
        }
    }

    private val activeState get() = state.value as? StoryCategoryContract.State.Active

    private fun expand(index: Int) {
        val s = activeState ?: return
        if (s.expandedIndex == index) {
            setState { (this as StoryCategoryContract.State.Active).copy(expandedIndex = null) }
            return
        }
        setState { (this as StoryCategoryContract.State.Active).copy(expandedIndex = index) }
    }

    private fun collapse() {
        setState { (this as StoryCategoryContract.State.Active).copy(expandedIndex = null) }
    }

    private fun changeSource(lang: LanguageItem) {
        setState {
            (this as StoryCategoryContract.State.Active).copy(langSource = lang, expandedIndex = null)
        }
        viewModelScope.launch { appPreferences.setSourceLang(lang.code) }
    }

    private fun changeTarget(lang: LanguageItem) {
        setState {
            (this as StoryCategoryContract.State.Active).copy(langTarget = lang, expandedIndex = null)
        }
        viewModelScope.launch { appPreferences.setTargetLang(lang.code) }
    }

    private fun swapLanguages() {
        val s = activeState ?: return
        setState {
            (this as StoryCategoryContract.State.Active).copy(
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

    private fun triggerTranslation(index: Int) {
        val s = activeState ?: return
        if (s.langTarget.code == "en") return

        val entry = s.stories.getOrNull(index) ?: return
        val cacheKey = "${s.langSource.code}:${s.langTarget.code}:${entry.id}"
        if (s.translationCache.containsKey(cacheKey)) return
        if (s.loadingIndices.contains(index)) return

        setState {
            (this as StoryCategoryContract.State.Active).copy(
                loadingIndices = loadingIndices + index,
                errorIndices = errorIndices - index
            )
        }

        val sourceLangAtDispatch = s.langSource.code
        val targetLangAtDispatch = s.langTarget.code

        viewModelScope.launch {
            val result = try {
                translateUseCase(entry.body, sourceLangAtDispatch, targetLangAtDispatch)
            } catch (e: Exception) {
                setState {
                    (this as StoryCategoryContract.State.Active).copy(
                        loadingIndices = loadingIndices - index,
                        errorIndices = errorIndices + index
                    )
                }
                return@launch
            }
            val current = activeState ?: return@launch
            val currentKey = "${current.langSource.code}:${current.langTarget.code}:${entry.id}"
            if (currentKey != cacheKey) return@launch  // language changed while in-flight

            when (result) {
                is NetworkResult.Success -> setState {
                    (this as StoryCategoryContract.State.Active).copy(
                        translationCache = translationCache + (cacheKey to result.data.translatedText),
                        loadingIndices = loadingIndices - index
                    )
                }
                is NetworkResult.Error -> setState {
                    (this as StoryCategoryContract.State.Active).copy(
                        loadingIndices = loadingIndices - index,
                        errorIndices = errorIndices + index
                    )
                }
            }
        }
    }

    private fun speakStory(index: Int) {
        val s = activeState ?: return
        val entry = s.stories.getOrNull(index) ?: return
        val cacheKey = "${s.langSource.code}:${s.langTarget.code}:${entry.id}"
        val text = s.translationCache[cacheKey] ?: entry.body
        val langCode = if (s.translationCache.containsKey(cacheKey)) s.langTarget.code else "en"
        sendEffect(StoryCategoryContract.Effect.SpeakText(text, langCode))
    }

    private fun copyStory(index: Int) {
        val s = activeState ?: return
        val entry = s.stories.getOrNull(index) ?: return
        val cacheKey = "${s.langSource.code}:${s.langTarget.code}:${entry.id}"
        val translation = s.translationCache[cacheKey]
        val text = if (translation != null) "${entry.title}\n\n${entry.body}\n\n---\n$translation"
                   else "${entry.title}\n\n${entry.body}"
        sendEffect(StoryCategoryContract.Effect.CopyToClipboard(text))
    }
}
