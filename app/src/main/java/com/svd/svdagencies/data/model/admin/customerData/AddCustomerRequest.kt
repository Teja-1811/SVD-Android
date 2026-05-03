package com.svd.svdagencies.data.model.admin.customerData

import com.google.gson.annotations.SerializedName

data class AddCustomerRequest(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("name") val name: String,
    @SerializedName("shop_name") val shop_name: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("city") val city: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("area") val area: String? = null,
    @SerializedName("pincode") val pincode: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("retailer_id") val retailer_id: String? = null
)
