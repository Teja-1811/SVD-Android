package com.svd.svdagencies.data.model.admin

data class ConfirmOrderResponse(
    val success: Boolean,
    val message: String?,
    val bill_id: Int?,
    val invoice_number: String?,
    val error: String?
)
