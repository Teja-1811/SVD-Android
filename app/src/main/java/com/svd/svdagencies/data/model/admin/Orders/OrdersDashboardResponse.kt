package com.svd.svdagencies.data.model.admin.Orders

data class OrdersDashboardResponse(
    val total_pending: Int,
    val orders: List<AdminOrder>
)