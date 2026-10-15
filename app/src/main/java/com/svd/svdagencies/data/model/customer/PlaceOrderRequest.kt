package com.svd.svdagencies.data.model.customer

import com.google.gson.annotations.SerializedName

data class PlaceOrderRequest(
    @SerializedName("items") val items: List<OrderItemRequest>,
    @SerializedName("order_type") val orderType: String = "regular",
    @SerializedName("delivery_date") val deliveryDate: String? = null
)

data class OrderItemRequest(
    @SerializedName("item_id") val itemId: Int,
    @SerializedName("quantity") val quantity: Double
)
