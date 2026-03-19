package com.android.zubanx.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.android.zubanx.data.local.datastore.AppPreferences
import com.android.zubanx.data.local.datastore.AppPreferencesImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zuban_prefs")

val dataStoreModule = module {
    single<DataStore<Preferences>> { androidContext().dataStore }
    single<AppPreferences> { AppPreferencesImpl(get()) }
}
