package com.svd.svdagencies.data.model.customer

import com.google.gson.annotations.SerializedName

data class PlaceOrderResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("order_number") val orderNumber: String?
)