package com.android.zubanx.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.android.zubanx.domain.model.PlanType
import com.android.zubanx.domain.model.PremiumPlan
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

class BillingManager(context: Context) : PurchasesUpdatedListener, BillingClientStateListener {

    private val _billingState = MutableStateFlow<BillingState>(BillingState.Loading)
    val billingState: StateFlow<BillingState> = _billingState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    private val productDetailsMap = mutableMapOf<String, ProductDetails>()

    fun connect() {
        if (billingClient.isReady) return
        billingClient.startConnection(this)
    }

    override fun onBillingSetupFinished(result: BillingResult) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            scope.launch { queryProducts() }
        } else {
            _billingState.value = BillingState.Error("Billing setup failed: ${result.debugMessage}")
        }
    }

    override fun onBillingServiceDisconnected() {
        Timber.w("BillingManager: service disconnected, retrying...")
        billingClient.startConnection(this)
    }

    private suspend fun queryProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PremiumProductIds.WEEKLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PremiumProductIds.MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PremiumProductIds.YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val details = result.productDetailsList ?: emptyList()
            productDetailsMap.clear()
            details.forEach { productDetailsMap[it.productId] = it }
            val plans = details.mapNotNull { it.toPremiumPlan() }
            _billingState.value = BillingState.Ready(plans)
        } else {
            _billingState.value = BillingState.Error("Failed to load products: ${result.billingResult.debugMessage}")
        }
    }

    fun launchPurchaseFlow(activity: Activity, plan: PremiumPlan) {
        val productDetails = productDetailsMap[plan.productId] ?: run {
            Timber.e("BillingManager: no ProductDetails for ${plan.productId}")
            return
        }
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            scope.launch {
                purchases
                    .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    .forEach { acknowledgePurchase(it) }
            }
        } else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            _billingState.value = BillingState.Error("Purchase failed: ${result.debugMessage}")
        }
    }

    private suspend fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) {
            _billingState.value = BillingState.Purchased
            return
        }
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        val result = billingClient.acknowledgePurchase(params)
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            _billingState.value = BillingState.Purchased
        } else {
            _billingState.value = BillingState.Error("Acknowledgment failed: ${result.debugMessage}")
        }
    }

    fun restorePurchases() {
        scope.launch {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
            val result = billingClient.queryPurchasesAsync(params)
            val activePurchase = result.purchasesList.firstOrNull {
                it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            if (activePurchase != null) {
                _billingState.value = BillingState.Purchased
            }
        }
    }

    private fun ProductDetails.toPremiumPlan(): PremiumPlan? {
        val price = subscriptionOfferDetails?.firstOrNull()
            ?.pricingPhases?.pricingPhaseList?.firstOrNull()
            ?.formattedPrice ?: return null
        val planType = when (productId) {
            PremiumProductIds.WEEKLY  -> PlanType.WEEKLY
            PremiumProductIds.MONTHLY -> PlanType.MONTHLY
            PremiumProductIds.YEARLY  -> PlanType.YEARLY
            else -> return null
        }
        return PremiumPlan(
            productId = productId,
            planType = planType,
            title = name,
            price = price,
            isDefault = planType == PlanType.WEEKLY
        )
    }
}
