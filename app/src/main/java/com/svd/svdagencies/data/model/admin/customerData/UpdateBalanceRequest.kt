package com.svd.svdagencies.data.model.admin.customerData

data class UpdateBalanceRequest(
    val amount: String
)

data class GenericSuccessResponse(
    val success: Boolean
)