package com.svd.svdagencies.data.model.admin

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Company(
    val id: Int,
    val name: String,
    val logo: String?,
    @SerializedName("website_link")
    val websiteLink: String?,
    @SerializedName("total_items")
    val totalItems: Int,
    @SerializedName("total_qty")
    val totalQty: Double,
    @SerializedName("total_value")
    val totalValue: Double
) : Parcelable


data class CompaniesListResponse(
    val count: Int,
    val companies: List<Company>
)
