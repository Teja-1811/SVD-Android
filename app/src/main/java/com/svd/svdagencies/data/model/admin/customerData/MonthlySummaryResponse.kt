package com.svd.svdagencies.data.model.admin.customerData

data class MonthlySummaryResponse(
    val year: Int,
    val month: Int,
    val days_in_month: Int,
    val customer: SummaryCustomer,
    val summary: SalesSummary,
    val volume: VolumeSummary,
    val statement_groups: List<MonthlyStatementGroup>? = null
)

data class SummaryCustomer(
    val id: Int?,
    val name: String?,
    val phone: String?,
    val shop_name: String? = null
)

data class SalesSummary(
    val total_sales: Double,
    val paid_amount: Double,
    val opening_due: Double,
    val due_amount: Double,
    val remaining_due: Double,
    val total_items: Int,
    val total_discount: Double = 0.0,
    val overall_customer_profit: Double = 0.0,
    val selected_period_profit: Double = 0.0
)

data class VolumeSummary(
    val milk_volume: Double,
    val curd_volume: Double,
    val total_volume: Double,
    val avg_milk_per_day: Double,
    val avg_curd_per_day: Double,
    val avg_total_per_day: Double
)



data class MonthlyStatementGroup(val date: String, val total: Double, val paid: Double, val due: Double, val lines: List<MonthlyStatementLine>?)
data class MonthlyStatementLine(val item_code: String?, val item_name: String?, val quantity: Double, val rate: Double, val amount: Double, val gst_rate: Double?, val gst_amount: Double?)
