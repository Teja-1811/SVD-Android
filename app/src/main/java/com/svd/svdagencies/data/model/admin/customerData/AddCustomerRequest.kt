package com.svd.svdagencies.data.model.admin.customerData

data class AddCustomerRequest(
    val customer_id: Int? = null,
    val name: String,
    val shop_name: String,
    val phone: String,
    val city: String,
    val state: String,
    val area: String? = null,
    val pincode: String? = null,
    val address: String? = null,
    val retailer_id: String
)