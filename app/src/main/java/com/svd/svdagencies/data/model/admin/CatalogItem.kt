package com.svd.svdagencies.data.model.admin

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class CatalogItem(
    val id: Int,
    val name: String,
    @SerializedName("selling_price")
    val sellingPrice: Double,
    @SerializedName("buying_price")
    val buyingPrice: Double = 0.0,
    val mrp: Double
) : Parcelable
