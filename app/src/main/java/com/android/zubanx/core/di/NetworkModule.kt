package com.android.zubanx.core.di

import com.android.zubanx.core.network.KtorClientFactory
import com.android.zubanx.data.remote.api.AiExpertService
import com.android.zubanx.data.remote.api.AiExpertServiceImpl
import com.android.zubanx.data.remote.api.DictionaryApiService
import com.android.zubanx.data.remote.api.DictionaryApiServiceImpl
import com.android.zubanx.data.remote.api.TranslateApiService
import com.android.zubanx.data.remote.api.TranslateApiServiceImpl
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val networkModule = module {
    // Singleton HttpClient — all services share one connection pool
    single { KtorClientFactory.create() }

    singleOf(::TranslateApiServiceImpl) bind TranslateApiService::class
    singleOf(::DictionaryApiServiceImpl) bind DictionaryApiService::class
    singleOf(::AiExpertServiceImpl) bind AiExpertService::class
}
