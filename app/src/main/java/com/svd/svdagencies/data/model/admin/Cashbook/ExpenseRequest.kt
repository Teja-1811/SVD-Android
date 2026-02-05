package com.svd.svdagencies.data.model.admin.Cashbook

data class ExpenseRequest(
    val amount: Double,
    val category: String,
    val description: String? = null
)