package com.svd.svdagencies.data.model.admin.Items

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class AdminItem(
    val id: Int,
    val code: String?,
    val name: String,
    val company: String?,
    @SerializedName("company_logo")
    val logo: String?,
    val category: String?,
    val selling_price: String?,
    val buying_price: String?,
    val mrp: String?,
    val stock_quantity: Int?,
    val pcs_count: Int?,
    val image: String?,
    val frozen: Boolean
) : Parcelable {
    val sellingPriceValue: Double
        get() = selling_price?.toDoubleOrNull() ?: 0.0

    val buyingPriceValue: Double
        get() = buying_price?.toDoubleOrNull() ?: 0.0

    val mrpValue: Double
        get() = mrp?.toDoubleOrNull() ?: 0.0

    val margin: Double
        get() = sellingPriceValue - buyingPriceValue
}