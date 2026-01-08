package com.svd.svdagencies.data.model.admin

data class ConfirmOrderRequest(
    val quantities: List<ConfirmOrderItem>
)

data class ConfirmOrderItem(
    val item_id: Int,
    val quantity: Int,
    val discount: Double
)
