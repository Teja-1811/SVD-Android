package com.svd.svdagencies.data.model.admin

data class BillItem(
    val id: Int,
    val invoice_number: String,
    val invoice_date: String,
    val customer: String,
    val productName: String,
    val price: Double,
    val quantity: Int,
    val discount: Double,
    val total_amount: String
)
