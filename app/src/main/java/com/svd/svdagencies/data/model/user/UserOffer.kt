package com.svd.svdagencies.data.model.user

import com.google.gson.annotations.SerializedName

data class UserOffer(
    val id: Int,
    val name: String,
    @SerializedName("offer_type")
    val offerType: String?,
    val price: Double?,
    val description: String?,
    @SerializedName("start_date")
    val startDate: String?,
    @SerializedName("end_date")
    val endDate: String?,
    val items: List<UserOfferItem> = emptyList()
)
