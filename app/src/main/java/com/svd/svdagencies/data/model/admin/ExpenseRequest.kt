package com.svd.svdagencies.data.model.admin

data class ExpenseRequest(
    val amount: Double,
    val category: String,
    val description: String? = null
)
