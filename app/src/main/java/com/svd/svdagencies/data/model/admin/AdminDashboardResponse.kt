package com.svd.svdagencies.data.model.admin

import androidx.annotation.Keep

@Keep
data class AdminDashboardResponse(
    val date: String? = "",
    val summary: DashboardSummary? = null,
    val customers_no_orders_today_list: List<NoOrderCustomer>? = emptyList(),
    val top_due_customers: List<TopDueCustomer>? = emptyList(),
    val top_stock_items: List<TopStockItem>? = emptyList(),
    val top_selling_items_today: List<Any>? = emptyList()
)

@Keep
data class DashboardSummary(
    val customers: Int? = 0,
    val active_customers: Int? = 0,
    val frozen_customers: Int? = 0,
    val retailers: Int? = 0,
    val users: Int? = 0,
    val delivery_users: Int? = 0,
    val items: Int? = 0,
    val active_items: Int? = 0,
    val sales_today: Double? = 0.0,
    val sales_month: Double? = 0.0,
    val sales_year: Double? = 0.0,
    val profit_today: Double? = 0.0,
    val dues: Double? = 0.0,
    val stock_value: Double? = 0.0,
    val low_stock_items: Int? = 0,
    val out_of_stock_items: Int? = 0,
    val pending_orders: Int? = 0,
    val today_orders: Int? = 0,
    val today_order_sales: Double? = 0.0,
    val active_enquiries: Int? = 0,
    val resolved_enquiries: Int? = 0,
    val active_subscriptions: Int? = 0,
    val expiring_subscriptions: Int? = 0,
    val active_offers: Int? = 0,
    val payments_today_total: Double? = 0.0,
    val payments_today_count: Int? = 0,
    val stock_in_today_value: Double? = 0.0,
    val stock_in_today_entries: Int? = 0,
    val leakage_month_loss: Double? = 0.0,
    val today_subscription_orders: Int? = 0,
    val today_subscription_delivered: Int? = 0,
    val customers_no_orders_today_count: Int? = 0
)

@Keep
data class NoOrderCustomer(
    val id: Int? = 0,
    val name: String? = "",
    val phone: String? = "",
    val shop_name: String? = ""
)

@Keep
data class TopDueCustomer(
    val id: Int? = 0,
    val name: String? = "",
    val phone: String? = "",
    val actual_due: Double? = 0.0
)

@Keep
data class TopStockItem(
    val id: Int? = 0,
    val name: String? = "",
    val stock_quantity: Int? = 0,
    val pcs_count: Int? = 0,
    val category: String? = "",
    val company_name: String? = "",
    val stock_value: Double? = 0.0
)
