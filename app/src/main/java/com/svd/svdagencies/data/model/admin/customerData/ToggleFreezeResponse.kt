package com.svd.svdagencies.data.model.admin.customerData

import com.google.gson.annotations.SerializedName

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
    @SerializedName("pin_code") val pincode: String?,
    @SerializedName("flat_number") val address: String?,
    val frozen: Boolean,
    val retailer_id: String?,
    val user_type: String? = "retailer"
)
