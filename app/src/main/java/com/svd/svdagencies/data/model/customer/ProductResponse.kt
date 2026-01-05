package com.svd.svdagencies.data.model.customer

data class ProductResponse(
    val id: Int,
    val name: String,
    val mrp: Double,
    val selling_price: Double,
    val margin: Double,
    val stock: Int,
    val image: String,
    val pcs_count: Int
)