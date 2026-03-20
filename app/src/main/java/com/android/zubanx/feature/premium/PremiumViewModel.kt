package com.android.zubanx.feature.premium

import androidx.lifecycle.viewModelScope
import com.android.zubanx.billing.BillingManager
import com.android.zubanx.billing.BillingState
import com.android.zubanx.core.mvi.BaseViewModel
import com.android.zubanx.data.local.datastore.AppPreferences
import com.android.zubanx.domain.model.PlanType
import kotlinx.coroutines.launch

class PremiumViewModel(
    private val billingManager: BillingManager,
    private val appPreferences: AppPreferences
) : BaseViewModel<PremiumContract.State, PremiumContract.Event, PremiumContract.Effect>(
    PremiumContract.State()
) {
    init {
        viewModelScope.launch {
            billingManager.billingState.collect { billingState ->
                when (billingState) {
                    is BillingState.Loading -> setState { copy(isLoading = true) }
                    is BillingState.Ready -> setState {
                        copy(
                            isLoading = false,
                            plans = billingState.plans,
                            selectedPlan = billingState.plans.firstOrNull { it.planType == PlanType.WEEKLY }
                        )
                    }
                    is BillingState.Purchased -> {
                        viewModelScope.launch { appPreferences.setIsPremium(true) }
                        setState { copy(isPremium = true) }
                        sendEffect(PremiumContract.Effect.NavigateBack)
                    }
                    is BillingState.Error -> setState {
                        copy(isLoading = false, errorMessage = billingState.message)
                    }
                }
            }
        }
        viewModelScope.launch {
            appPreferences.isPremium.collect { isPremium ->
                setState { copy(isPremium = isPremium) }
            }
        }
    }

    override fun onEvent(event: PremiumContract.Event) {
        when (event) {
            is PremiumContract.Event.SelectPlan -> setState { copy(selectedPlan = event.plan) }
            PremiumContract.Event.Purchase -> {
                val plan = state.value.selectedPlan ?: return
                setState { copy(isPurchasing = true) }
                sendEffect(PremiumContract.Effect.LaunchBillingFlow(plan))
            }
            PremiumContract.Event.RestorePurchase -> billingManager.restorePurchases()
            PremiumContract.Event.OpenPrivacyPolicy -> sendEffect(PremiumContract.Effect.OpenUrl("https://zubanx.app/privacy"))
            PremiumContract.Event.OpenTerms -> sendEffect(PremiumContract.Effect.OpenUrl("https://zubanx.app/terms"))
        }
    }
}
