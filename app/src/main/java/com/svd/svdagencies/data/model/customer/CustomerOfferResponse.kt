package com.svd.svdagencies.data.model.customer

import com.google.gson.annotations.SerializedName

data class CustomerOfferResponse(
    val status: Boolean,
    val count: Int,
    val offers: List<OfferItem>
)

data class OfferItem(
    val id: Int,
    val name: String,
    @SerializedName("offer_type") val offerType: String,
    val price: Double,
    val description: String?,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    val items: List<OfferProductItem>
)

data class OfferProductItem(
    @SerializedName("item_id") val itemId: Int,
    @SerializedName("item_name") val itemName: String,
    @SerializedName("buy_qty") val buyQty: Int,
    @SerializedName("offer_qty") val offerQty: Int,
    @SerializedName("offer_price") val offerPrice: Double
)
