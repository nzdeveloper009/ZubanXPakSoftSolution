package com.android.zubanx.feature.premium

import android.content.Intent
import android.net.Uri
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.android.zubanx.billing.BillingManager
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.core.utils.collectFlow
import com.android.zubanx.core.utils.toast
import com.android.zubanx.databinding.FragmentPremiumBinding
import com.android.zubanx.domain.model.PremiumPlan
import com.android.zubanx.domain.model.PlanType
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class PremiumFragment : BaseFragment<FragmentPremiumBinding>(FragmentPremiumBinding::inflate) {

    private val viewModel: PremiumViewModel by viewModel()
    private val billingManager: BillingManager by inject()

    override fun setupViews() {
        binding.btnSubscribe.setOnClickListener {
            viewModel.onEvent(PremiumContract.Event.Purchase)
        }
        binding.btnRestorePurchase.setOnClickListener {
            viewModel.onEvent(PremiumContract.Event.RestorePurchase)
        }

        binding.cardWeekly.setOnClickListener {
            val plan = viewModel.state.value.plans.firstOrNull { it.planType == PlanType.WEEKLY }
            plan?.let { viewModel.onEvent(PremiumContract.Event.SelectPlan(it)) }
        }
        binding.cardMonthly.setOnClickListener {
            val plan = viewModel.state.value.plans.firstOrNull { it.planType == PlanType.MONTHLY }
            plan?.let { viewModel.onEvent(PremiumContract.Event.SelectPlan(it)) }
        }
        binding.cardYearly.setOnClickListener {
            val plan = viewModel.state.value.plans.firstOrNull { it.planType == PlanType.YEARLY }
            plan?.let { viewModel.onEvent(PremiumContract.Event.SelectPlan(it)) }
        }

        binding.tvPrivacyPolicy.setOnClickListener {
            viewModel.onEvent(PremiumContract.Event.OpenPrivacyPolicy)
        }
        binding.tvTerms.setOnClickListener {
            viewModel.onEvent(PremiumContract.Event.OpenTerms)
        }
    }

    override fun observeState() {
        collectFlow(viewModel.state) { state ->
            binding.progressLoading.isVisible = state.isLoading
            binding.btnSubscribe.isEnabled = !state.isLoading && !state.isPurchasing && state.selectedPlan != null
            binding.progressPurchasing.isVisible = state.isPurchasing

            state.plans.forEach { plan ->
                val card = when (plan.planType) {
                    PlanType.WEEKLY -> binding.cardWeekly
                    PlanType.MONTHLY -> binding.cardMonthly
                    PlanType.YEARLY -> binding.cardYearly
                }
                val priceView = when (plan.planType) {
                    PlanType.WEEKLY -> binding.tvPriceWeekly
                    PlanType.MONTHLY -> binding.tvPriceMonthly
                    PlanType.YEARLY -> binding.tvPriceYearly
                }
                priceView.text = plan.price
                val isSelected = state.selectedPlan?.productId == plan.productId
                card.strokeWidth = if (isSelected) 4 else 0
            }

        }

        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                is PremiumContract.Effect.LaunchBillingFlow -> {
                    billingManager.launchPurchaseFlow(requireActivity(), effect.plan)
                }
                is PremiumContract.Effect.OpenUrl -> {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(effect.url)))
                }
                is PremiumContract.Effect.ShowToast -> requireContext().toast(effect.message)
                PremiumContract.Effect.NavigateBack -> findNavController().navigateUp()
            }
        }
    }
}
