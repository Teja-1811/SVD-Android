package com.svd.svdagencies.data.model.admin.Orders

data class ConfirmOrderRequest(
    val quantities: List<ConfirmOrderItem>
)

data class ConfirmOrderItem(
    val item_id: Int,
    val quantity: Int,
    val discount: Double
)
