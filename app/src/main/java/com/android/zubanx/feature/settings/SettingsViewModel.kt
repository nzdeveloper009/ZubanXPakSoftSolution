package com.android.zubanx.feature.settings

import androidx.lifecycle.viewModelScope
import com.android.zubanx.BuildConfig
import com.android.zubanx.R
import com.android.zubanx.core.mvi.BaseViewModel
import com.android.zubanx.data.local.datastore.AppPreferences
import com.android.zubanx.domain.model.AiTone
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val appPreferences: AppPreferences
) : BaseViewModel<SettingsContract.State, SettingsContract.Event, SettingsContract.Effect>(
    SettingsContract.State(appVersion = BuildConfig.VERSION_NAME)
) {
    init {
        viewModelScope.launch {
            combine(
                appPreferences.isPremium,
                appPreferences.offlineMode,
                appPreferences.autoSpeak,
                appPreferences.floatingOverlay
            ) { isPremium, offlineMode, autoSpeak, floatingOverlay ->
                SettingsContract.State(
                    isPremium = isPremium,
                    offlineMode = offlineMode,
                    floatingOverlay = floatingOverlay,
                    autoSpeak = autoSpeak,
                    appVersion = BuildConfig.VERSION_NAME
                )
            }.collect { newState -> setState { newState } }
        }
        viewModelScope.launch {
            appPreferences.aiTone.collect { key ->
                setState { copy(aiTone = AiTone.fromKey(key)) }
            }
        }
    }

    override fun onEvent(event: SettingsContract.Event) {
        when (event) {
            SettingsContract.Event.NavigateToPremium ->
                sendEffect(SettingsContract.Effect.Navigate(R.id.action_settings_to_premium))
            SettingsContract.Event.NavigateToFavourites ->
                sendEffect(SettingsContract.Effect.Navigate(R.id.action_settings_to_favourite))
            SettingsContract.Event.NavigateToHistory ->
                sendEffect(SettingsContract.Effect.ShowToast("History coming soon"))
            SettingsContract.Event.ShowAiTonePicker ->
                sendEffect(SettingsContract.Effect.ShowAiToneDialog(state.value.aiTone))
            is SettingsContract.Event.SetAiTone -> viewModelScope.launch {
                appPreferences.setAiTone(event.tone.key)
            }
            SettingsContract.Event.NavigateToLanguage ->
                // TODO: Uncomment after Task 12 adds action_settings_to_language to nav_settings.xml
                sendEffect(SettingsContract.Effect.ShowToast("Language settings coming soon"))
            SettingsContract.Event.OpenPrivacyPolicy ->
                sendEffect(SettingsContract.Effect.OpenUrl("https://zubanx.app/privacy"))
            SettingsContract.Event.OpenTerms ->
                sendEffect(SettingsContract.Effect.OpenUrl("https://zubanx.app/terms"))
            SettingsContract.Event.RateUs ->
                sendEffect(SettingsContract.Effect.LaunchRateUs)
            SettingsContract.Event.ShareApp ->
                sendEffect(SettingsContract.Effect.LaunchShare)
            SettingsContract.Event.ContactSupport ->
                sendEffect(SettingsContract.Effect.LaunchContactSupport)
            is SettingsContract.Event.SetOfflineMode -> viewModelScope.launch {
                appPreferences.setOfflineMode(event.enabled)
            }
            is SettingsContract.Event.SetFloatingOverlay -> {
                sendEffect(SettingsContract.Effect.StartFloatingService(event.enabled))
                viewModelScope.launch { appPreferences.setFloatingOverlay(event.enabled) }
            }
            is SettingsContract.Event.SetAutoSpeak -> viewModelScope.launch {
                appPreferences.setAutoSpeak(event.enabled)
            }
        }
    }
}
