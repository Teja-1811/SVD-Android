package com.svd.svdagencies.data.model.admin.Orders

import com.svd.svdagencies.data.model.admin.Orders.AdminOrderItem

data class AdminOrder(
    val order_id: Int,
    val order_date: String,
    val customer_id: Int?,
    val customer_name: String,
    val total_amount: Double,
    val items_count: Int,
    val items: List<AdminOrderItem> = emptyList()
)

data class AdminOrderDetail(
    val order_id: Int,
    val order_date: String,
    val customer_name: String,
    val status: String,
    val total_amount: Double,
    val items: List<AdminOrderItem>
)

data class AdminOrderItem(
    val order_item_id: Int,
    val item_id: Int,
    val item_name: String,
    val price: Double,
    val requested_quantity: Int,
    val discount_per_qty: Double,
    val discount_total: Double,
    val requested_total: Double
)