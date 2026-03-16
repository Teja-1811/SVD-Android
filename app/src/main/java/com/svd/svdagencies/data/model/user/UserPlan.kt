package com.svd.svdagencies.data.model.user

data class UserPlan(
    val id: Int,
    val name: String,
    val price: Double,
    val description: String?,
    val items: List<UserSubscriptionItem>? = emptyList()
)
