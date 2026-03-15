package com.svd.svdagencies.data.model.user

import com.google.gson.annotations.SerializedName

data class UserOfferItem(
    @SerializedName("item_id")
    val itemId: Int?,
    @SerializedName("item_name")
    val itemName: String?,
    @SerializedName("buy_qty")
    val buyQty: Int?,
    @SerializedName("offer_qty")
    val offerQty: Int?,
    @SerializedName("offer_price")
    val offerPrice: Double?
)
