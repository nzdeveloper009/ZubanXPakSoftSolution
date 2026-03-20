package com.android.zubanx.domain.model

data class PremiumPlan(
    val productId: String,
    val planType: PlanType,
    val title: String,
    val price: String,
    val isDefault: Boolean = false
)

enum class PlanType { WEEKLY, MONTHLY, YEARLY }
