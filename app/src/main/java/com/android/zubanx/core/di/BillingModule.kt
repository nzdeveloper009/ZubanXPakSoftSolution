package com.android.zubanx.core.di

import com.android.zubanx.billing.BillingManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val billingModule = module {
    singleOf(::BillingManager)
}
