package com.svd.svdagencies.data.model.customer

import com.google.gson.annotations.SerializedName

data class CustomerStatementResponse(
    @SerializedName("customer_id") val customerId: Int,
    @SerializedName("customer_name") val customerName: String,
    @SerializedName("customer_phone") val customerPhone: String?,
    val year: Int,
    val month: Int,
    val period: StatementPeriod,
    val summary: StatementSummary,
    val invoices: List<StatementInvoice>,
    val payments: List<StatementPayment>,
    @SerializedName("monthly_statement") val monthlyStatement: MonthlyStatementSummary? = null,
    @SerializedName("statement_groups") val statementGroups: List<MonthlyStatementGroup>? = emptyList()
)

data class MonthlyStatementSummary(
    @SerializedName("opening_due") val openingDue: Double,
    @SerializedName("total_sales") val totalSales: Double,
    @SerializedName("paid_amount") val paidAmount: Double,
    @SerializedName("monthly_due") val monthlyDue: Double,
    @SerializedName("remaining_due") val remainingDue: Double,
    @SerializedName("total_items") val totalItems: Double,
    @SerializedName("total_discount") val totalDiscount: Double,
    @SerializedName("average_daily_sales") val averageDailySales: Double,
)

data class MonthlyStatementGroup(
    val date: String,
    @SerializedName("invoice_number") val invoiceNumber: String,
    @SerializedName("opening_due") val openingDue: Double,
    val total: Double,
    val paid: Double,
    val due: Double,
    val items: List<MonthlyStatementItem> = emptyList(),
)

data class MonthlyStatementItem(
    @SerializedName("item_code") val itemCode: String,
    @SerializedName("item_name") val itemName: String,
    val quantity: Double,
    val rate: Double,
    val discount: Double,
    val amount: Double,
)

data class StatementPeriod(
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String
)

data class StatementSummary(
    @SerializedName("opening_due") val openingDue: Double,
    @SerializedName("total_billed") val totalBilled: Double,
    @SerializedName("total_paid") val totalPaid: Double,
    @SerializedName("closing_due") val closingDue: Double,
    @SerializedName("invoice_count") val invoiceCount: Int,
    @SerializedName("payment_count") val paymentCount: Int
)

data class StatementInvoice(
    @SerializedName("bill_id") val billId: Int,
    @SerializedName("invoice_number") val invoiceNumber: String,
    @SerializedName("invoice_date") val invoiceDate: String,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("opening_due") val openingDue: Double,
    @SerializedName("last_paid") val lastPaid: Double
)

data class StatementPayment(
    @SerializedName("payment_id") val paymentId: Int,
    @SerializedName("transaction_id") val transactionId: String,
    val method: String,
    @SerializedName("payment_for") val paymentFor: String,
    val amount: Double,
    val status: String,
    @SerializedName("completed_at") val completedAt: String?
)
