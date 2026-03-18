package com.svd.svdagencies.data.model.user

import com.google.gson.annotations.SerializedName

data class PrebookOrderRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("delivery_date") val deliveryDate: String,
    @SerializedName("slot") val slot: String,
    @SerializedName("notes") val notes: String?,
    @SerializedName("items") val items: List<PrebookItem>
)

data class PrebookItem(
    @SerializedName("item_id") val itemId: Int,
    @SerializedName("quantity") val quantity: Double
)
