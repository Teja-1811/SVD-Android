package com.svd.svdagencies.data.model.user

import com.google.gson.annotations.SerializedName

data class UserSubscription(
    val id: Int?,
    @SerializedName("plan_id")
    val planId: Int?,
    @SerializedName("plan")
    val planName: String?,
    val price: Double = 0.0,
    val description: String?,
    @SerializedName("duration_in_days")
    val durationInDays: Int = 0,
    @SerializedName("start_date")
    val startDate: String?,
    @SerializedName("end_date")
    val endDate: String?,
    @SerializedName("is_active")
    val isActive: Boolean = false,
    val items: List<UserSubscriptionItem> = emptyList()
)
