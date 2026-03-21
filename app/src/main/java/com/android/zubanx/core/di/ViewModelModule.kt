package com.android.zubanx.core.di

import com.android.zubanx.feature.dictionary.DictionaryViewModel
import com.android.zubanx.feature.dictionary.WordDetailViewModel
import com.android.zubanx.feature.favourite.FavouriteViewModel
import com.android.zubanx.feature.onboarding.OnboardingViewModel
import com.android.zubanx.feature.premium.PremiumViewModel
import com.android.zubanx.feature.settings.SettingsViewModel
import com.android.zubanx.feature.splash.SplashViewModel
import com.android.zubanx.feature.conversation.ConversationViewModel
import com.android.zubanx.feature.translate.TranslateViewModel
import com.android.zubanx.feature.phrases.PhrasesViewModel
import com.android.zubanx.feature.phrases.PhrasesCategoryViewModel
import com.android.zubanx.feature.language.LanguageViewModel
import com.android.zubanx.feature.idioms.IdiomsViewModel
import com.android.zubanx.feature.idioms.IdiomsCategoryViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::SplashViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::TranslateViewModel)
    viewModelOf(::DictionaryViewModel)
    viewModelOf(::WordDetailViewModel)
    viewModelOf(::ConversationViewModel)
    viewModelOf(::FavouriteViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::PremiumViewModel)
    viewModelOf(::PhrasesViewModel)
    viewModel { params -> PhrasesCategoryViewModel(get(), get(), params.get()) }
    viewModelOf(::LanguageViewModel)
    viewModelOf(::IdiomsViewModel)
    viewModel { params -> IdiomsCategoryViewModel(get(), get(), params.get()) }
}
