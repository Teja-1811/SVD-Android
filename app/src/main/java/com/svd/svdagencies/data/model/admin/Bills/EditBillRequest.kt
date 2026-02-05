package com.svd.svdagencies.data.model.admin.Bills

import com.google.gson.annotations.SerializedName

data class EditBillRequest(
    val items: List<Int>,
    val quantities: List<Int>,
    val discounts: List<Double>,
    @SerializedName("customer") val customerId: Int? = null,
    @SerializedName("invoice_date") val invoiceDate: String? = null
)