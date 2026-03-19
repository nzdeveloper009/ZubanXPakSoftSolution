package com.android.zubanx.core.di

import com.android.zubanx.feature.onboarding.OnboardingViewModel
import com.android.zubanx.feature.splash.SplashViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Populated per feature plan
val viewModelModule = module {
    viewModelOf(::SplashViewModel)
    viewModelOf(::OnboardingViewModel)
}
