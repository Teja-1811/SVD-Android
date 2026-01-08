package com.svd.svdagencies.data.model.admin

data class MonthlyPaymentSummaryResponse(
    val total_invoice: Double,
    val total_paid: Double,
    val total_due: Double
)
