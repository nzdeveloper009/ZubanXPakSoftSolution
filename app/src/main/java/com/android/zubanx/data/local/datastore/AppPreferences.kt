package com.android.zubanx.data.local.datastore

import kotlinx.coroutines.flow.Flow

interface AppPreferences {
    val theme: Flow<String>
    val selectedExpert: Flow<String>
    val sourceLang: Flow<String>
    val targetLang: Flow<String>
    val isPremium: Flow<Boolean>
    val offlineMode: Flow<Boolean>
    val onboardingComplete: Flow<Boolean>

    suspend fun setTheme(value: String)
    suspend fun setSelectedExpert(value: String)
    suspend fun setSourceLang(value: String)
    suspend fun setTargetLang(value: String)
    suspend fun setIsPremium(value: Boolean)
    suspend fun setOfflineMode(value: Boolean)
    suspend fun setOnboardingComplete(value: Boolean)
}
