package com.android.zubanx.core.di

import com.android.zubanx.feature.onboarding.OnboardingViewModel
import com.android.zubanx.feature.splash.SplashViewModel
import com.android.zubanx.feature.translate.TranslateViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::SplashViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::TranslateViewModel)
}
