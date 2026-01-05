package com.svd.svdagencies.data.model.admin

data class UpdateBalanceResponse(
    val success: Boolean,
    val new_balance: Double,
    val message: String? = null
)
