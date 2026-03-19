package com.android.zubanx.core.di

import com.android.zubanx.data.mlkit.MlKitOcrService
import com.android.zubanx.data.mlkit.MlKitTranslateService
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val mlKitModule = module {
    // TextRecognizer is created via a static ML Kit factory
    single { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    singleOf(::MlKitOcrService)
    singleOf(::MlKitTranslateService)
}
