package com.svd.svdagencies.data.model.admin

data class OrdersDashboardResponse(
    val total_pending: Int,
    val orders: List<AdminOrder>
)
