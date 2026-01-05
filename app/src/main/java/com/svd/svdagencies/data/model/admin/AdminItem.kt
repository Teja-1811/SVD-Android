package com.svd.svdagencies.data.model.admin

data class AdminItem(
    val id: Int,
    val name: String,
    val code: String,
    val company: String,
    val buying_price: Double,
    val selling_price: Double,
    val mrp: Double,
    val margin: Double,
    val stock: Int,
    val image_url: String? = null,
    val category: String
)
