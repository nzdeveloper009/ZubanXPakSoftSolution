package com.android.zubanx.core.di

import com.android.zubanx.feature.dictionary.DictionaryViewModel
import com.android.zubanx.feature.dictionary.WordDetailViewModel
import com.android.zubanx.feature.favourite.FavouriteViewModel
import com.android.zubanx.feature.onboarding.OnboardingViewModel
import com.android.zubanx.feature.premium.PremiumViewModel
import com.android.zubanx.feature.splash.SplashViewModel
import com.android.zubanx.feature.conversation.ConversationViewModel
import com.android.zubanx.feature.translate.TranslateViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::SplashViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::TranslateViewModel)
    viewModelOf(::DictionaryViewModel)
    viewModelOf(::WordDetailViewModel)
    viewModelOf(::ConversationViewModel)
    viewModelOf(::FavouriteViewModel)
    viewModelOf(::PremiumViewModel)
}
