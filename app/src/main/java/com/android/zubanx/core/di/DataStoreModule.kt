package com.android.zubanx.core.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.android.zubanx.core.utils.zubanDataStore
import com.android.zubanx.data.local.datastore.AppPreferences
import com.android.zubanx.data.local.datastore.AppPreferencesImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val dataStoreModule = module {
    single<DataStore<Preferences>> { androidContext().zubanDataStore }
    singleOf(::AppPreferencesImpl) bind AppPreferences::class
}
