package com.svd.svdagencies.data.model.admin

data class ExpenseListResponse(
    val expenses: List<ExpenseItem>,
    val total_expenses: Double
)

data class ExpenseItem(
    val id: Int,
    val date: String,
    val category: String,
    val amount: Double,
    val description: String
)
