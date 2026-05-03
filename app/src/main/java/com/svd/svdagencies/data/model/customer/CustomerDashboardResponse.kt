package com.svd.svdagencies.data.model.customer

import com.google.gson.annotations.SerializedName

data class CustomerDashboardResponse(
    val customer: CustomerDashboardCustomer,
    val summary: CustomerDashboardSummary,
    @SerializedName("latest_bill")
    val latestBill: CustomerDashboardLatestBill? = null,
    @SerializedName("latest_order")
    val latestOrder: CustomerDashboardLatestOrder? = null
)

data class CustomerDashboardCustomer(
    val id: Int,
    val name: String,
    @SerializedName("shop_name")
    val shopName: String? = null,
    val phone: String? = null,
    val area: String? = null,
    val city: String? = null,
    val state: String? = null,
    @SerializedName("pin_code")
    val pinCode: String? = null,
    @SerializedName("account_status")
    val accountStatus: String? = null,
    val frozen: Boolean = false
)

data class CustomerDashboardSummary(
    val balance: Double = 0.0,
    @SerializedName("outstanding_due")
    val outstandingDue: Double = 0.0,
    @SerializedName("wallet_balance")
    val walletBalance: Double = 0.0,
    @SerializedName("monthly_invoice_count")
    val monthlyInvoiceCount: Int = 0,
    @SerializedName("monthly_spend")
    val monthlySpend: Double = 0.0,
    @SerializedName("active_tickets")
    val activeTickets: Int = 0
)

data class CustomerDashboardLatestBill(
    @SerializedName("bill_id")
    val billId: Int,
    @SerializedName("invoice_number")
    val invoiceNumber: String,
    @SerializedName("invoice_date")
    val invoiceDate: String,
    @SerializedName("total_amount")
    val totalAmount: Double = 0.0
)

data class CustomerDashboardLatestOrder(
    @SerializedName("order_id")
    val orderId: Int,
    @SerializedName("order_number")
    val orderNumber: String,
    @SerializedName("order_date")
    val orderDate: String,
    @SerializedName("delivery_date")
    val deliveryDate: String? = null,
    val status: String,
    @SerializedName("total_amount")
    val totalAmount: Double = 0.0,
    @SerializedName("approved_total_amount")
    val approvedTotalAmount: Double = 0.0,
    @SerializedName("delivery_charge")
    val deliveryCharge: Double = 0.0,
    @SerializedName("payment_status")
    val paymentStatus: String? = null
)
