package com.android.zubanx.core.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

internal val Context.zubanDataStore: DataStore<Preferences> by preferencesDataStore(name = "zuban_prefs")
