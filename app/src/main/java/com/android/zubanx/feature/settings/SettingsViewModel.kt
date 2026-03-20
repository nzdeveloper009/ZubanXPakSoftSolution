package com.android.zubanx.feature.settings

import androidx.lifecycle.viewModelScope
import com.android.zubanx.BuildConfig
import com.android.zubanx.R
import com.android.zubanx.core.mvi.BaseViewModel
import com.android.zubanx.data.local.datastore.AppPreferences
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
                appPreferences.floatingOverlay,
                appPreferences.selectedExpert
            ) { isPremium, offlineMode, autoSpeak, floatingOverlay, expert ->
                SettingsContract.State(
                    isPremium = isPremium,
                    aiTone = expert,
                    offlineMode = offlineMode,
                    floatingOverlay = floatingOverlay,
                    autoSpeak = autoSpeak,
                    appVersion = BuildConfig.VERSION_NAME
                )
            }.collect { newState ->
                setState { newState }
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
            SettingsContract.Event.NavigateToAiTone ->
                sendEffect(SettingsContract.Effect.ShowToast("AI Tone coming soon"))
            SettingsContract.Event.NavigateToLanguage ->
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
