package com.svd.svdagencies.data.model.admin

data class PaymentsDashboardResponse(
    val year: Int,
    val month: Int,
    val payments: List<CompanyPayment>,
    val grand_total_invoice: Double,
    val grand_total_paid: Double,
    val grand_total_due: Double
)

data class CompanyPayment(
    val company_id: Int,
    val company_name: String,
    val records: List<DailyRecord>,
    val total_invoice: Double,
    val total_paid: Double,
    val remaining_due: Double
)

data class DailyRecord(
    val date: String,
    val invoice_amount: Double,
    val paid_amount: Double
)
