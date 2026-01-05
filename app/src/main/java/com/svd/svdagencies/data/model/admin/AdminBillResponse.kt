package com.svd.svdagencies.data.model.admin

data class AdminBillResponse(
    val status: String,
    val bills: List<AdminBill>
)
