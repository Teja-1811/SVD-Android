package com.svd.svdagencies.data.model.customer

data class InvoiceResponse(
    val invoices: List<InvoiceItem>? = emptyList()
)

data class InvoiceItem(
    val number: String? = "",
    val date: String? = "",
    val amount: Double = 0.0,
    val id: Int = 0,
    @com.google.gson.annotations.SerializedName("remaining_due") val remainingDue: Double = 0.0,
    @com.google.gson.annotations.SerializedName("last_paid") val paid: Double = 0.0,
    @com.google.gson.annotations.SerializedName("opening_due") val openingDue: Double = 0.0,
    val items: List<InvoiceLineItem>? = emptyList(),
)

data class InvoiceLineItem(
    val code: String? = "",
    val quantity: Double = 0.0,
    val rate: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0,
)

