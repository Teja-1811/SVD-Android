package com.svd.svdagencies.data.model.admin.Bills

import com.google.gson.annotations.SerializedName

data class AdminBill(
    val id: Int,
    @SerializedName("invoice_number")
    val bill_number: String?,
    @SerializedName("customer")
    val customer_name: String?,
    @SerializedName("invoice_date")
    val date: String?,
    @SerializedName("total_amount")
    val total_amount: Double = 0.0,
    @SerializedName("profit")
    val profit: Double = 0.0,
    @SerializedName("file_url")
    val file_url: String? = null
)
