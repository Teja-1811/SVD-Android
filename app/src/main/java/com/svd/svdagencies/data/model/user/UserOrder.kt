package com.svd.svdagencies.data.model.user

import com.google.gson.annotations.SerializedName

data class UserOrderDetailResponse(
    @SerializedName("order") val order: UserOrderDetail,
    @SerializedName("items") val items: List<UserOrderItem>
)

data class UserOrderDetail(
    @SerializedName("id") val id: Int,
    @SerializedName("order_number") val orderNumber: String,
    @SerializedName("order_date") val orderDate: String,
    @SerializedName("delivery_date") val deliveryDate: String?,
    @SerializedName("status") val status: String,
    @SerializedName("delivery_address") val deliveryAddress: String?,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("delivery_charge") val deliveryCharge: Double,
    @SerializedName("approved_total_amount") val approvedTotalAmount: Double,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class UserOrderItem(
    @SerializedName("item_id") val itemId: Int,
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("requested_quantity") val requestedQuantity: Int,
    @SerializedName("requested_price") val requestedPrice: Double,
    @SerializedName("approved_quantity") val approvedQuantity: Int,
    @SerializedName("approved_price") val approvedPrice: Double,
    @SerializedName("discount") val discount: Double,
    @SerializedName("discount_total") val discountTotal: Double,
    @SerializedName("requested_total") val requestedTotal: Double,
    @SerializedName("approved_total") val approvedTotal: Double
)
