package com.android.zubanx.core.di

import com.android.zubanx.security.KeyDecryptionModule
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val securityModule = module {
    // FirebaseRemoteConfig uses a static getInstance() — wrap in a plain single block
    single<FirebaseRemoteConfig> { FirebaseRemoteConfig.getInstance() }

    singleOf(::KeyDecryptionModule)
}
