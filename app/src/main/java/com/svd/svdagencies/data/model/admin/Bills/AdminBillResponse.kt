package com.svd.svdagencies.data.model.admin.Bills

data class AdminBillResponse(
    val status: String,
    val bills: List<AdminBill>
)