package com.svd.svdagencies.data.model.customer

import com.google.gson.annotations.SerializedName
import com.svd.svdagencies.data.model.delivery.DeliveryCustomerPaymentRecord

data class GenericResponse(
    val status: String,
    @SerializedName("payment_id") val paymentId: Int? = null,
    @SerializedName("payment_for") val paymentFor: String? = null,
    @SerializedName("verification_required") val verificationRequired: Boolean? = null,
    val new_balance: String? = null,
    @SerializedName("payment_history") val paymentHistory: List<DeliveryCustomerPaymentRecord>? = null,
    val error: String? = null
)
