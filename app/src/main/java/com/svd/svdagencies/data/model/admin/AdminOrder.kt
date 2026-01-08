package com.svd.svdagencies.data.model.admin

data class AdminOrder(
    val order_id: Int,
    val order_date: String,
    val customer_id: Int?,
    val customer_name: String,
    val total_amount: Double,
    val items_count: Int
)
