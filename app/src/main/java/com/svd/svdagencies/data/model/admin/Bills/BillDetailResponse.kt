package com.svd.svdagencies.data.model.admin.Bills

import com.google.gson.annotations.SerializedName

data class BillDetailResponse(
    val id: Int,
    val invoice_number: String,
    val invoice_date: String,
    val customer: String?,
    @SerializedName(value = "invoice_amount", alternate = ["total_amount"])
    val invoiceAmount: Double,
    @SerializedName(value = "op_due", alternate = ["op_due_amount"])
    val openingDue: Double,
    @SerializedName(value = "paid", alternate = ["last_paid", "paid_amount"])
    val paid: Double,
    @SerializedName("remaining_due") val remainingDue: Double = 0.0,
    val current_due: Double,
    val profit: Double,
    val items: List<BillItemDetail>? = null
) {
    val total_amount: Double get() = invoiceAmount
    val op_due_amount: Double get() = openingDue
    val last_paid: Double get() = paid
}
