package com.svd.svdagencies.data.model.user

import com.google.gson.annotations.SerializedName

data class AutoUpiPayload(
    @SerializedName("is_active")
    val isActive: Boolean = false,
    @SerializedName("upi_id")
    val upiId: String?,
    @SerializedName("max_amount")
    val maxAmount: Double = 0.0,
    @SerializedName("last_payment_amount")
    val lastPaymentAmount: Double = 0.0,
    @SerializedName("last_payment_date")
    val lastPaymentDate: String?
)
