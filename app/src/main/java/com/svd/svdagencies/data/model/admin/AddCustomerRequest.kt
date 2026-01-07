package com.svd.svdagencies.data.model.admin

data class AddCustomerRequest(
    val customer_id: Int? = null,
    val name: String,
    val shop_name: String,
    val phone: String,
    val city: String,
    val state: String,
    val retailer_id: String
)
