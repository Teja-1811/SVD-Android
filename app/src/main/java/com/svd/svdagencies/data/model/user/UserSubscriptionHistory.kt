package com.svd.svdagencies.data.model.user

import com.google.gson.annotations.SerializedName

data class UserSubscriptionHistory(
    val plan: String,
    @SerializedName("start_date")
    val startDate: String?,
    @SerializedName("end_date")
    val endDate: String?,
    val status: String
)
