package com.android.zubanx.core.di

import com.android.zubanx.data.repository.*
import com.android.zubanx.domain.repository.*
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::TranslationRepositoryImpl) bind TranslationRepository::class
    singleOf(::FavouriteRepositoryImpl) bind FavouriteRepository::class
    singleOf(::DictionaryRepositoryImpl) bind DictionaryRepository::class
    singleOf(::OfflineLanguagePackRepositoryImpl) bind OfflineLanguagePackRepository::class
}
