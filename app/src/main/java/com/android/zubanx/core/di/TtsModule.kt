package com.android.zubanx.core.di

import com.android.zubanx.tts.SttManager
import com.android.zubanx.tts.TtsManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val ttsModule = module {
    // Context-based construction — Android framework static factories require androidContext()
    single { TtsManager(androidContext()) }
    single { SttManager(androidContext()) }
}
