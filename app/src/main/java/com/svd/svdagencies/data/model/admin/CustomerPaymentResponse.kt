package com.svd.svdagencies.data.model.admin

data class CustomerPaymentResponse(
    val count: Int,
    val payments: List<CustomerPaymentItem>
)

data class CustomerPaymentItem(
    val id: Int,
    val transaction_id: String?,
    val amount: Double,
    val payment_mode: String?,
    val status: String?,
    val created_at: String?,
    val customer_name: String?,
    val customer_phone: String?
)
