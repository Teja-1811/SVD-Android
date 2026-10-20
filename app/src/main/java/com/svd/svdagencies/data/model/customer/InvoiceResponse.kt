package com.svd.svdagencies.data.model.customer

data class InvoiceResponse(
    val invoices: List<InvoiceItem>
)

data class InvoiceItem(
    val number: String,
    val date: String,
    val amount: Double,
    val id: Int = 0,
    @com.google.gson.annotations.SerializedName("remaining_due") val remainingDue: Double = 0.0
)
