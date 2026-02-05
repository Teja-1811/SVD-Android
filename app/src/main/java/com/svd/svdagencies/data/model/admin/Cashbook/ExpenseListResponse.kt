package com.svd.svdagencies.data.model.admin.Cashbook

data class ExpenseListResponse(
    val expenses: List<Expense>,
    val total_expenses: Double
)

data class Expense(
    val id: Int,
    val date: String,
    val category: String,
    val amount: Double,
    val description: String
)
