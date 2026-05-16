package com.svd.svdagencies.data.model.admin.Bills

data class BillDetailResponse(
    val id: Int,
    val invoice_number: String,
    val invoice_date: String,
    val customer: String?,
    val total_amount: Double,
    val op_due_amount: Double,
    val last_paid: Double,
    val current_due: Double,
    val profit: Double,
    val items: List<com.svd.svdagencies.data.model.user.UserBillItem>? = null
)