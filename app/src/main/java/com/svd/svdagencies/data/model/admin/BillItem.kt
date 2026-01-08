package com.svd.svdagencies.data.model.admin

data class BillItem(
    val id: Int,
    val invoice_number: String,
    val invoice_date: String,
    val customer: String,
    val total_amount: String,
    val op_due: String,
    val current_due: String
)
