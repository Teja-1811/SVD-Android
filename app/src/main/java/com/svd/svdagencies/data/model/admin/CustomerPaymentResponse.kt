package com.svd.svdagencies.data.model.admin

import com.google.gson.annotations.SerializedName

data class CustomerPaymentResponse(
    val count: Int,
    val payments: List<CustomerPaymentItem>
)

data class CustomerPaymentItem(
    val id: Int,
    val transaction_id: String?,
    val amount: Double,
    @SerializedName("method")
    val payment_mode: String?,
    val status: String?,
    val created_at: String?,
    val customer_name: String?,
    val customer_phone: String?
)
