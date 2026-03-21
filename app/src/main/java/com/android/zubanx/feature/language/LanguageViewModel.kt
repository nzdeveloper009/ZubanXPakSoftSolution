package com.android.zubanx.feature.language

import androidx.lifecycle.viewModelScope
import com.android.zubanx.core.mvi.BaseViewModel
import com.android.zubanx.data.local.datastore.AppPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LanguageViewModel(
    private val appPreferences: AppPreferences
) : BaseViewModel<LanguageContract.State, LanguageContract.Event, LanguageContract.Effect>(
    LanguageContract.State()
) {
    init {
        viewModelScope.launch {
            val current = appPreferences.appLanguage.first()
            setState { copy(selectedCode = current) }
        }
    }

    override fun onEvent(event: LanguageContract.Event) {
        when (event) {
            is LanguageContract.Event.SelectLanguage ->
                setState { copy(selectedCode = event.code) }
            LanguageContract.Event.Confirm -> viewModelScope.launch {
                val code = state.value.selectedCode
                appPreferences.setAppLanguage(code)
                sendEffect(LanguageContract.Effect.ApplyLocaleAndRestart(code))
            }
        }
    }
}
