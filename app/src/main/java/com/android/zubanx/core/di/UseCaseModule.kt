package com.android.zubanx.core.di

import com.android.zubanx.domain.usecase.dictionary.EnrichWithAiUseCase
import com.android.zubanx.domain.usecase.dictionary.GetDictionaryHistoryUseCase
import com.android.zubanx.domain.usecase.dictionary.LookupWordUseCase
import com.android.zubanx.domain.usecase.translate.AddFavouriteFromTranslationUseCase
import com.android.zubanx.domain.usecase.translate.DeleteTranslationUseCase
import com.android.zubanx.domain.usecase.translate.GetTranslationHistoryUseCase
import com.android.zubanx.domain.usecase.conversation.ConversationTranslateUseCase
import com.android.zubanx.domain.usecase.translate.TranslateUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val useCaseModule = module {
    factoryOf(::TranslateUseCase)
    factoryOf(::GetTranslationHistoryUseCase)
    factoryOf(::DeleteTranslationUseCase)
    factoryOf(::AddFavouriteFromTranslationUseCase)
    factoryOf(::LookupWordUseCase)
    factoryOf(::GetDictionaryHistoryUseCase)
    factoryOf(::EnrichWithAiUseCase)
    factoryOf(::ConversationTranslateUseCase)
}
