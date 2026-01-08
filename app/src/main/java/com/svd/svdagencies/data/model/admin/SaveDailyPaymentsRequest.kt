package com.svd.svdagencies.data.model.admin

data class SaveDailyPaymentsRequest(
    val year: Int,
    val month: Int,
    val data: Map<String, Map<String, PaymentData>>
)

data class PaymentData(
    val invoice: Double?,
    val paid: Double?
)
