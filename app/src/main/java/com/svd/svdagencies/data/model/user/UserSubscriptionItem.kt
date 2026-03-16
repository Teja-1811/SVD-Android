package com.svd.svdagencies.data.model.user

import com.google.gson.annotations.SerializedName

data class UserSubscriptionItem(
    @SerializedName("item_id")
    val itemId: Int,
    @SerializedName("item_name")
    val itemName: String,
    val quantity: Double = 0.0,
    val price: Double? = null,
    val per: String? = null
)
