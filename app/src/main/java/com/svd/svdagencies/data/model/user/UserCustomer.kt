package com.svd.svdagencies.data.model.user

import com.google.gson.annotations.SerializedName

data class UserCustomer(
    val id: Int,
    val name: String,
    val phone: String?,
    @SerializedName("shop_name")
    val shopName: String?,
    @SerializedName("retailer_id")
    val retailerId: String?,
    @SerializedName("flat_number")
    val flatNumber: String?,
    val area: String?,
    val city: String?,
    val state: String?,
    @SerializedName("pin_code")
    val pinCode: String?,
    @SerializedName("account_status")
    val accountStatus: String?,
    @SerializedName("is_commissioned")
    val isCommissioned: Boolean = false,
    @SerializedName("is_delivery")
    val isDelivery: Boolean = false,
    val frozen: Boolean = false,
    @SerializedName("user_type")
    val userType: String?,
    val due: Double = 0.0
)
