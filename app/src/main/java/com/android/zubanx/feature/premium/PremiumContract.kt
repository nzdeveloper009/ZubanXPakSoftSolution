package com.android.zubanx.feature.premium

import com.android.zubanx.core.mvi.UiEffect
import com.android.zubanx.core.mvi.UiEvent
import com.android.zubanx.core.mvi.UiState
import com.android.zubanx.domain.model.PremiumPlan

object PremiumContract {
    data class State(
        val plans: List<PremiumPlan> = emptyList(),
        val selectedPlan: PremiumPlan? = null,
        val isPurchasing: Boolean = false,
        val isPremium: Boolean = false,
        val isLoading: Boolean = true,
        val errorMessage: String? = null
    ) : UiState

    sealed class Event : UiEvent {
        data class SelectPlan(val plan: PremiumPlan) : Event()
        object Purchase : Event()
        object RestorePurchase : Event()
        object OpenPrivacyPolicy : Event()
        object OpenTerms : Event()
    }

    sealed class Effect : UiEffect {
        data class LaunchBillingFlow(val plan: PremiumPlan) : Effect()
        data class OpenUrl(val url: String) : Effect()
        data class ShowToast(val message: String) : Effect()
        object NavigateBack : Effect()
    }
}
