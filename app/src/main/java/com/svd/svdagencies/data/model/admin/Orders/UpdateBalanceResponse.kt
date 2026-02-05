package com.svd.svdagencies.data.model.admin.Orders

data class UpdateBalanceResponse(
    val success: Boolean,
    val new_balance: Double,
    val message: String? = null
)