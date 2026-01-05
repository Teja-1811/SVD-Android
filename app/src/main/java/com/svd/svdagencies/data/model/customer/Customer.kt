package com.svd.svdagencies.data.model.customer

import com.google.gson.annotations.SerializedName

data class Customer(
    @SerializedName("id")
    val id: Int? = 0,
    @SerializedName("name")
    val name: String? = "",
    @SerializedName("shop_name")
    val shop_name: String? = "",
    @SerializedName("balance")
    val balance: Double? = 0.0,
    @SerializedName("phone")
    val phone: String? = "",
    @SerializedName("is_active")
    val is_active: Boolean? = true
)