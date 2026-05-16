package com.svd.svdagencies.data.model.admin.customerData

data class ToggleFreezeResponse(
    val success: Boolean,
    val frozen: Boolean
)

data class CustomerDetail(
    val id: Int,
    val name: String,
    val shop_name: String,
    val phone: String,
    val due: Double,
    val city: String?,
    val state: String?,
    val area: String?,
    val route_id: Int? = null,
    val route_name: String? = null,
    val pincode: String?,
    val address: String?,
    val frozen: Boolean,
    val retailer_id: String?
)
