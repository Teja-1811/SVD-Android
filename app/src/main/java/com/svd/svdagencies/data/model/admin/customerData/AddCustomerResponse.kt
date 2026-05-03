package com.svd.svdagencies.data.model.admin.customerData

import com.google.gson.annotations.SerializedName

data class AddCustomerResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String
)
