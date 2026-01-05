package com.svd.svdagencies.data.model.admin

data class AdminDashboardResponse(
    val customers: Int? = 0,
    val items: Int? = 0,
    val sales_today: Double? = 0.0,
    val dues: Double? = 0.0,
    val pending_orders: Int? = 0,
    val customers_no_orders_today_count: Int? = 0,
    val customers_no_orders_today_list: List<NoOrderCustomer>? = emptyList()
)

data class NoOrderCustomer(
    val id: Int? = 0,
    val name: String? = "",
    val phone: String? = ""
)
