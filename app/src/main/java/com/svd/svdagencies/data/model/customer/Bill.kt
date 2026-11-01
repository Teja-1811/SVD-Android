package com.svd.svdagencies.data.model.customer

import com.google.gson.annotations.SerializedName

data class Bill(
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
