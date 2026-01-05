package com.svd.svdagencies.data.model.customer

import com.google.gson.annotations.SerializedName

data class CustomerDashboardResponse(
    @SerializedName("customerName")
    val customerName: String,
    @SerializedName("balance")
    val balance: Double,
    @SerializedName("shopName")
    val shopName: String,
    @SerializedName("phone")
    val phone: String,
    @SerializedName("accountStatus")
    val accountStatus: String
)