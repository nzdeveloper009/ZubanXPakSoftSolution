package com.android.zubanx.billing

import com.android.zubanx.domain.model.PremiumPlan

sealed interface BillingState {
    object Loading : BillingState
    data class Ready(val plans: List<PremiumPlan>) : BillingState
    object Purchased : BillingState
    data class Error(val message: String) : BillingState
}
