package com.svd.svdagencies.data.model.user

import com.google.gson.annotations.SerializedName

data class UserDashboardResponse(
    val status: Boolean,
    val customer: UserCustomer,
    val subscription: UserSubscription,
    @SerializedName("subscription_history")
    val subscriptionHistory: List<UserSubscriptionHistory> = emptyList(),
    @SerializedName("subscription_pauses")
    val subscriptionPauses: List<UserSubscriptionPause> = emptyList(),
    val offers: List<UserOffer> = emptyList(),
    @SerializedName("auto_upi")
    val autoUpi: AutoUpiPayload? = null
)
