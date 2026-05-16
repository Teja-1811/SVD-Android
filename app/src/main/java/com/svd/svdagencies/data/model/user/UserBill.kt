package com.svd.svdagencies.data.model.user

import com.google.gson.annotations.SerializedName

data class UserBillsResponse(
    @SerializedName("bills") val bills: List<UserBill>
)

data class UserBill(
    @SerializedName("id") val id: Int,
    @SerializedName("invoice_number") val invoiceNumber: String,
    @SerializedName("invoice_date") val invoiceDate: String,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("opening_due", alternate = ["op_due_amount"]) val openingDue: Double,
    @SerializedName("profit") val profit: Double,
    @SerializedName("current_due") val currentDue: Double
)

data class UserBillDetailResponse(
    @SerializedName("bill") val bill: UserBillDetail,
    @SerializedName("items") val items: List<UserBillItem>
)

data class UserBillDetail(
    @SerializedName("id") val id: Int,
    @SerializedName("invoice_number") val invoiceNumber: String,
    @SerializedName("invoice_date") val invoiceDate: String,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("opening_due", alternate = ["op_due_amount"]) val openingDue: Double,
    @SerializedName("last_paid") val lastPaid: Double,
    @SerializedName("profit") val profit: Double,
    @SerializedName("current_due") val currentDue: Double
)

data class UserBillItem(
    @SerializedName("item_id") val itemId: Int,
    @SerializedName("code", alternate = ["item_code"]) val code: String = "",
    @SerializedName("name", alternate = ["item_name"]) val name: String = "",
    @SerializedName("mrp") val mrp: Double = 0.0,
    @SerializedName("price_per_unit") val pricePerUnit: Double,
    @SerializedName("discount") val discount: Double,
    @SerializedName("total_discount") val totalDiscount: Double = 0.0,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("total_amount") val totalAmount: Double
)
