package com.svd.svdagencies.data.model.delivery

import com.google.gson.annotations.SerializedName

data class DeliveryItem(
    val type: String, // "order" or "subscription"
    val id: Int,
    @SerializedName("order_number") val orderNumber: String? = null,
    @SerializedName("customer_name") val customerName: String,
    @SerializedName("delivery_date") val deliveryDate: String? = null,
    val date: String? = null, // for subscriptions
    val status: String,
    @SerializedName("total_amount") val totalAmount: Double? = null,
    val address: String? = null,
    @SerializedName("plan_item") val planItem: String? = null,
    val quantity: Int? = null
)

data class DeliveryTodayResponse(
    val pending: List<DeliveryItem>,
    val completed: List<DeliveryItem>
)

data class DeliveryUpdateRequest(
    val type: String,
    @SerializedName("delivery_id") val deliveryId: Int? = null,
    val id: Int? = null,
    @SerializedName("order_id") val orderId: Int? = null,
    @SerializedName("subscription_order_id") val subscriptionOrderId: Int? = null,
    val status: String? = null,
    @SerializedName("delivered_amount") val deliveredAmount: Double? = null,
    val eta: String? = null,
    @SerializedName("delivered_at") val deliveredAt: String? = null,
    val notes: String? = null
)

data class DeliveryUpdateResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("delivery_id") val deliveryId: Int? = null
)
