package com.svd.svdagencies.data.model.admin.Bills

import com.google.gson.annotations.SerializedName

data class BillItem(
    val id: Int,
    val invoice_number: String,
    val invoice_date: String,
    val customer: String,
    val productName: String,
    val price: Double,
    @SerializedName(value = "quantity", alternate = ["qty"])
    val quantity: Double,
    val discount: Double,
    val total_amount: String
)
