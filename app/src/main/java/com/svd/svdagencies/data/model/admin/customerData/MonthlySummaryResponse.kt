package com.svd.svdagencies.data.model.admin.customerData

data class MonthlySummaryResponse(
    val year: Int,
    val month: Int,
    val days_in_month: Int,
    val customer: SummaryCustomer,
    val summary: SalesSummary,
    val volume: VolumeSummary,
    val commission: CommissionSummary
)

data class SummaryCustomer(
    val id: Int?,
    val name: String?,
    val phone: String?
)

data class SalesSummary(
    val total_sales: Double,
    val paid_amount: Double,
    val opening_due: Double,
    val due_amount: Double,
    val remaining_due: Double,
    val total_items: Int
)

data class VolumeSummary(
    val milk_volume: Double,
    val curd_volume: Double,
    val total_volume: Double,
    val avg_milk_per_day: Double,
    val avg_curd_per_day: Double,
    val avg_total_per_day: Double
)

data class CommissionSummary(
    val milk_commission: Double,
    val curd_commission: Double,
    val total_commission: Double
)
