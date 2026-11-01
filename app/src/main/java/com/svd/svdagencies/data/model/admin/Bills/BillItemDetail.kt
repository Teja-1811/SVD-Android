package com.svd.svdagencies.data.model.admin.Bills

import com.google.gson.annotations.SerializedName

data class BillItemDetail(
    val item_id: Int,
    val item_name: String,
    @SerializedName(value = "quantity", alternate = ["qty"])
    val quantity: Double,
    val price_per_unit: Double,
    val discount: Double,
    val total_discount: Double,
    val total_amount: Double
)
