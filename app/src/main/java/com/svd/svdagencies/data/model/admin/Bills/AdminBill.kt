package com.svd.svdagencies.data.model.admin.Bills

import com.google.gson.annotations.SerializedName

data class AdminBill(
    val id: Int,
    @SerializedName("invoice_number")
    val bill_number: String?,
    @SerializedName("customer")
    val customer_name: String?,
    @SerializedName("invoice_date")
    val date: String?,
    @SerializedName("total_amount")
    val total_amount: Double = 0.0,
    @SerializedName("current_due")
    val currentDue: Double = 0.0,
    @SerializedName("customer_phone")
    val customerPhone: String? = null,
    @SerializedName("customer_shop_name")
    val customerShopName: String? = null,
    @SerializedName("public_invoice_url")
    val publicInvoiceUrl: String? = null,
    @SerializedName("profit")
    val profit: Double = 0.0,
    @SerializedName("generated_by")
    val generatedBy: BillGenerator? = null,
    @SerializedName("file_url")
    val file_url: String? = null
)

data class BillGenerator(
    val id: Int? = null,
    val name: String? = null,
    val phone: String? = null,
    @SerializedName("user_type") val userType: String? = null
)
