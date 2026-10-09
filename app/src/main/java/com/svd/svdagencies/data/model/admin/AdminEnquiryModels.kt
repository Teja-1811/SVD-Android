package com.svd.svdagencies.data.model.admin

import com.google.gson.annotations.SerializedName

data class AdminEnquiriesResponse(
    val success: Boolean,
    val count: Int = 0,
    val enquiries: List<AdminEnquiry> = emptyList()
)

data class AdminEnquiry(
    val id: Int,
    val name: String,
    val phone: String,
    val email: String = "",
    val subject: String,
    val message: String,
    val status: String,
    @SerializedName("created_at")
    val createdAt: String
)

data class AdminEnquiryStatusResponse(
    val success: Boolean,
    val message: String,
    val enquiry: AdminEnquiry? = null
)
