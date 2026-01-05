package com.svd.svdagencies.data.model.admin

data class CustomerDetail(
    val id: Int,
    val name: String,
    val shop_name: String,
    val phone: String,
    val due: Double,
    val city: String?,
    val state: String?,
    val frozen: Boolean,
    val retailer_id: String?
)
