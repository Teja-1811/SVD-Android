package com.svd.svdagencies.data.model.customer

data class InvoiceResponse(
    val invoices: List<InvoiceItem>
)

data class InvoiceItem(
    val number: String,
    val date: String,
    val amount: Double
)
