package com.android.zubanx.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppPreferencesImpl(private val dataStore: DataStore<Preferences>) : AppPreferences {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val SELECTED_EXPERT = stringPreferencesKey("selected_expert")
        val SOURCE_LANG = stringPreferencesKey("source_lang")
        val TARGET_LANG = stringPreferencesKey("target_lang")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val OFFLINE_MODE = booleanPreferencesKey("offline_mode")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        val FLOATING_OVERLAY = booleanPreferencesKey("floating_overlay")
    }

    override val theme: Flow<String> = dataStore.data.map { it[Keys.THEME] ?: "SYSTEM" }
    override val selectedExpert: Flow<String> = dataStore.data.map { it[Keys.SELECTED_EXPERT] ?: "DEFAULT" }
    override val sourceLang: Flow<String> = dataStore.data.map { it[Keys.SOURCE_LANG] ?: "en" }
    override val targetLang: Flow<String> = dataStore.data.map { it[Keys.TARGET_LANG] ?: "es" }
    override val isPremium: Flow<Boolean> = dataStore.data.map { it[Keys.IS_PREMIUM] ?: false }
    override val offlineMode: Flow<Boolean> = dataStore.data.map { it[Keys.OFFLINE_MODE] ?: false }
    override val onboardingComplete: Flow<Boolean> = dataStore.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }
    override val autoSpeak: Flow<Boolean> = dataStore.data.map { it[Keys.AUTO_SPEAK] ?: false }
    override val floatingOverlay: Flow<Boolean> = dataStore.data.map { it[Keys.FLOATING_OVERLAY] ?: false }

    override suspend fun setTheme(value: String) { dataStore.edit { it[Keys.THEME] = value } }
    override suspend fun setSelectedExpert(value: String) { dataStore.edit { it[Keys.SELECTED_EXPERT] = value } }
    override suspend fun setSourceLang(value: String) { dataStore.edit { it[Keys.SOURCE_LANG] = value } }
    override suspend fun setTargetLang(value: String) { dataStore.edit { it[Keys.TARGET_LANG] = value } }
    override suspend fun setIsPremium(value: Boolean) { dataStore.edit { it[Keys.IS_PREMIUM] = value } }
    override suspend fun setOfflineMode(value: Boolean) { dataStore.edit { it[Keys.OFFLINE_MODE] = value } }
    override suspend fun setOnboardingComplete(value: Boolean) { dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = value } }
    override suspend fun setAutoSpeak(value: Boolean) { dataStore.edit { it[Keys.AUTO_SPEAK] = value } }
    override suspend fun setFloatingOverlay(value: Boolean) { dataStore.edit { it[Keys.FLOATING_OVERLAY] = value } }
}
