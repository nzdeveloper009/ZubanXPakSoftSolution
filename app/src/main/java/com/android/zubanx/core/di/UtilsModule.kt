package com.android.zubanx.core.di

import android.content.Context
import android.net.ConnectivityManager
import com.android.zubanx.core.utils.ConnectivityUtils
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val utilsModule = module {
    single<ConnectivityManager> {
        androidContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    singleOf(::ConnectivityUtils)
}
