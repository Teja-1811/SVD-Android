package com.svd.svdagencies.data.model.customer

import com.google.gson.annotations.SerializedName

data class PlaceOrderResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("order_number") val orderNumber: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("current_day") val currentDay: String? = null,
    @SerializedName("items") val items: List<PlacedOrderItem>? = emptyList()
)

data class CurrentDayOrdersResponse(
    @SerializedName("date") val date: String? = null,
    @SerializedName("orders") val orders: List<CustomerOrderPayload> = emptyList()
)

data class PlacedOrderItem(
    @SerializedName("item_id") val itemId: Int,
    @SerializedName(value = "name", alternate = ["item_name"]) val name: String? = null,
    @SerializedName("company") val company: String? = null,
    @SerializedName(value = "quantity", alternate = ["requested_quantity"]) val quantity: Double,
    @SerializedName(value = "unit_price", alternate = ["price", "requested_price"]) val unitPrice: Double? = null,
    @SerializedName(value = "total", alternate = ["requested_total"]) val total: Double? = null
)

data class CustomerOrderPayload(
    @SerializedName("order_number") val orderNumber: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("order_date") val orderDate: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("total_amount") val totalAmount: Double? = null,
    @SerializedName("items") val items: List<PlacedOrderItem>? = emptyList()
)
