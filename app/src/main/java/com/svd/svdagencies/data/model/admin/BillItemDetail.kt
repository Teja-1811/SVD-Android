package com.svd.svdagencies.data.model.admin

data class BillItemDetail(
    val item_name: String,
    val quantity: Int,
    val price_per_unit: Double,
    val discount: Double,
    val total_amount: Double
)
