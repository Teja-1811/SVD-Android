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
    val mrp: Double
) : Parcelable
