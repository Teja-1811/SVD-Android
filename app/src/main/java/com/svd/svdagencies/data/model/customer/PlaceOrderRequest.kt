package com.svd.svdagencies.data.model.customer

import com.google.gson.annotations.SerializedName

data class PlaceOrderRequest(
    @SerializedName("items") val items: List<OrderItemRequest>,
    @SerializedName("order_type") val orderType: String = "regular",
    @SerializedName("delivery_date") val deliveryDate: String? = null,
    @SerializedName("delivery_latitude") val deliveryLatitude: Double? = null,
    @SerializedName("delivery_longitude") val deliveryLongitude: Double? = null,
    @SerializedName("offer_id") val offerId: Int? = null,
    @SerializedName("offer_price") val offerPrice: Double? = null
)

data class OrderItemRequest(
    @SerializedName("item_id") val itemId: Int,
    @SerializedName("quantity") val quantity: Double
)
