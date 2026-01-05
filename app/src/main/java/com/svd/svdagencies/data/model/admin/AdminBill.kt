package com.svd.svdagencies.data.model.admin

data class AdminBill(
    val id: Int,
    val bill_number: String,
    val customer_name: String,
    val date: String,
    val total_amount: Double,
    val profit: Double,
    val file_url: String? = null
)
