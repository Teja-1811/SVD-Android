package com.svd.svdagencies.data.model.delivery

import com.google.gson.annotations.SerializedName

data class DeliveryAgentDuesResponse(
    val summary: DeliveryAgentDuesSummary = DeliveryAgentDuesSummary(),
    val agents: List<DeliveryAgentInfo> = emptyList(),
    val agent: DeliveryAgentInfo? = null,
    val items: List<DeliveryAgentItemSummary> = emptyList(),
    val results: List<DeliveryAgentDuesResult> = emptyList(),
    @SerializedName("customer_payment_records") val customerPaymentRecords: List<DeliveryCustomerPaymentRecord> = emptyList(),
    @SerializedName("daily_dues") val dailyDues: List<DeliveryAgentDailyDueRecord> = emptyList(),
    @SerializedName("bill_rows") val billRows: List<DeliveryAgentBillSummary> = emptyList(),
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("end_date") val endDate: String? = null
)

data class DeliveryAgentDuesSummary(
    @SerializedName("bill_count") val billCount: Int = 0,
    @SerializedName("total_amount") val totalAmount: Double = 0.0,
    @SerializedName("total_paid") val totalPaid: Double = 0.0,
    @SerializedName("total_due") val totalDue: Double = 0.0,
    @SerializedName("agent_current_due") val agentCurrentDue: Double = 0.0,
    @SerializedName("related_bill_count") val relatedBillCount: Int = 0,
    @SerializedName("related_invoice_amount") val relatedInvoiceAmount: Double = 0.0,
    @SerializedName("related_paid_amount") val relatedPaidAmount: Double = 0.0,
    @SerializedName("related_due_amount") val relatedDueAmount: Double = 0.0,
    @SerializedName("generated_bill_count") val generatedBillCount: Int = 0,
    @SerializedName("customer_bill_count") val customerBillCount: Int = 0,
    @SerializedName("self_bill_count") val selfBillCount: Int = 0,
    @SerializedName("generated_invoice_amount") val generatedInvoiceAmount: Double = 0.0,
    @SerializedName("customer_bill_amount") val customerBillAmount: Double = 0.0,
    @SerializedName("submitted_amount") val submittedAmount: Double = 0.0,
    @SerializedName("amount_to_submit") val amountToSubmit: Double = 0.0,
    @SerializedName("need_to_submit_amount") val needToSubmitAmount: Double = 0.0,
    @SerializedName("collected_amount") val collectedAmount: Double = 0.0,
    @SerializedName("self_bill_amount") val selfBillAmount: Double = 0.0,
    @SerializedName("remaining_generated_amount") val remainingGeneratedAmount: Double = 0.0,
    @SerializedName("counter_submit_amount") val counterSubmitAmount: Double = 0.0,
    @SerializedName("total_profit") val totalProfit: Double = 0.0,
    @SerializedName("salary_earned") val salaryEarned: Double = 0.0,
    @SerializedName("salary_paid") val salaryPaid: Double = 0.0,
    @SerializedName("remaining_salary") val remainingSalary: Double = 0.0,
    @SerializedName("pending_salary") val pendingSalary: Double = 0.0,
    @SerializedName("total_invoice_amount") val totalInvoiceAmount: Double = 0.0,
    @SerializedName("total_paid_amount") val totalPaidAmount: Double = 0.0,
    @SerializedName("total_counter_due") val totalCounterDue: Double = 0.0,
    @SerializedName("total_due_amount") val totalDueAmount: Double = 0.0,
    @SerializedName("counter_due") val counterDue: Double = 0.0,
    @SerializedName("holding_amount") val holdingAmount: Double = 0.0
)

data class DeliveryAgentInfo(
    val id: Int,
    val name: String? = null,
    val phone: String? = null,
    @SerializedName("user_type") val userType: String? = null
)

data class DeliveryAgentDuesResult(
    @SerializedName("delivery_boy_id") val deliveryBoyId: Int,
    @SerializedName("delivery_boy_name") val deliveryBoyName: String? = null,
    @SerializedName("delivery_boy_phone") val deliveryBoyPhone: String? = null,
    @SerializedName("bill_count") val billCount: Int = 0,
    @SerializedName("total_amount") val totalAmount: Double = 0.0,
    @SerializedName("total_paid") val totalPaid: Double = 0.0,
    @SerializedName("total_due") val totalDue: Double = 0.0,
    @SerializedName("agent_current_due") val agentCurrentDue: Double = 0.0,
    @SerializedName("related_bill_count") val relatedBillCount: Int = 0,
    @SerializedName("related_invoice_amount") val relatedInvoiceAmount: Double = 0.0,
    @SerializedName("related_paid_amount") val relatedPaidAmount: Double = 0.0,
    @SerializedName("related_due_amount") val relatedDueAmount: Double = 0.0,
    @SerializedName("generated_bill_count") val generatedBillCount: Int = 0,
    @SerializedName("customer_bill_count") val customerBillCount: Int = 0,
    @SerializedName("self_bill_count") val selfBillCount: Int = 0,
    @SerializedName("generated_invoice_amount") val generatedInvoiceAmount: Double = 0.0,
    @SerializedName("customer_bill_amount") val customerBillAmount: Double = 0.0,
    @SerializedName("submitted_amount") val submittedAmount: Double = 0.0,
    @SerializedName("amount_to_submit") val amountToSubmit: Double = 0.0,
    @SerializedName("need_to_submit_amount") val needToSubmitAmount: Double = 0.0,
    @SerializedName("collected_amount") val collectedAmount: Double = 0.0,
    @SerializedName("self_bill_amount") val selfBillAmount: Double = 0.0,
    @SerializedName("remaining_generated_amount") val remainingGeneratedAmount: Double = 0.0,
    @SerializedName("counter_submit_amount") val counterSubmitAmount: Double = 0.0,
    @SerializedName("total_profit") val totalProfit: Double = 0.0,
    @SerializedName("salary_earned") val salaryEarned: Double = 0.0,
    @SerializedName("salary_paid") val salaryPaid: Double = 0.0,
    @SerializedName("remaining_salary") val remainingSalary: Double = 0.0,
    @SerializedName("pending_salary") val pendingSalary: Double = 0.0,
    @SerializedName("total_invoice_amount") val totalInvoiceAmount: Double = 0.0,
    @SerializedName("total_paid_amount") val totalPaidAmount: Double = 0.0,
    @SerializedName("total_counter_due") val totalCounterDue: Double = 0.0,
    @SerializedName("total_due_amount") val totalDueAmount: Double = 0.0,
    @SerializedName("counter_due") val counterDue: Double = 0.0,
    @SerializedName("holding_amount") val holdingAmount: Double = 0.0,
    @SerializedName("salary_payments") val salaryPayments: List<DeliveryAgentSalaryPayment> = emptyList(),
    @SerializedName("submission_records") val submissionRecords: List<DeliveryAgentSubmissionRecord> = emptyList(),
    @SerializedName("payment_records") val paymentRecords: List<DeliveryCustomerPaymentRecord> = emptyList(),
    val bills: List<DeliveryAgentBillSummary> = emptyList()
)

data class DeliveryAgentSalaryPayment(
    val id: Int,
    @SerializedName("delivery_agent_id") val deliveryAgentId: Int,
    @SerializedName("delivery_agent_name") val deliveryAgentName: String? = null,
    val amount: Double = 0.0,
    @SerializedName("payment_date") val paymentDate: String? = null,
    val notes: String? = null
)

data class DeliveryAgentSubmissionRecord(
    val id: Int,
    @SerializedName("delivery_agent_id") val deliveryAgentId: Int,
    @SerializedName("delivery_agent_name") val deliveryAgentName: String? = null,
    @SerializedName("report_date") val reportDate: String? = null,
    @SerializedName("total_amount_to_submit") val totalAmountToSubmit: Double = 0.0,
    @SerializedName("submitted_amount") val submittedAmount: Double = 0.0,
    val notes: String? = null,
    @SerializedName("created_by") val createdBy: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class DeliveryCustomerPaymentRecord(
    val id: Int,
    @SerializedName("customer_id") val customerId: Int? = null,
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("customer_phone") val customerPhone: String? = null,
    @SerializedName("bill_id") val billId: Int? = null,
    @SerializedName("invoice_number") val invoiceNumber: String? = null,
    val amount: Double = 0.0,
    val method: String? = null,
    val status: String? = null,
    @SerializedName("payment_for") val paymentFor: String? = null,
    @SerializedName("transaction_id") val transactionId: String? = null,
    @SerializedName("payment_order_id") val paymentOrderId: String? = null,
    @SerializedName("gateway_transaction_id") val gatewayTransactionId: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class DeliveryAgentDailyDueRecord(
    val id: Int,
    val date: String,
    @SerializedName("amount_collected") val amountCollected: Double = 0.0,
    @SerializedName("settled_amount") val settledAmount: Double = 0.0,
    @SerializedName("balance_due") val balanceDue: Double = 0.0
)

data class DeliveryAgentBillSummary(
    val id: Int,
    @SerializedName("invoice_number") val invoiceNumber: String? = null,
    @SerializedName("invoice_date") val invoiceDate: String? = null,
    @SerializedName("customer_id") val customerId: Int? = null,
    val customer: String? = null,
    @SerializedName("total_amount") val totalAmount: Double = 0.0,
    @SerializedName(value = "opening_due", alternate = ["op_due"])
    val openingDue: Double = 0.0,
    @SerializedName("paid_amount") val paidAmount: Double = 0.0,
    @SerializedName("due_amount") val dueAmount: Double = 0.0,
    @SerializedName("remaining_due") val remainingDue: Double = 0.0,
    @SerializedName("is_self_bill") val isSelfBill: Boolean = false,
    val profit: Double = 0.0,
    val items: List<DeliveryAgentBillItem> = emptyList(),
    @SerializedName("file_url") val fileUrl: String? = null,
    @SerializedName("public_invoice_url") val publicInvoiceUrl: String? = null,
    @SerializedName("customer_phone") val customerPhone: String? = null,
    @SerializedName("customer_shop_name") val customerShopName: String? = null
)

data class DeliveryAgentBillItem(
    @SerializedName("bill_item_id") val billItemId: Int,
    @SerializedName("item_id") val itemId: Int,
    @SerializedName("item_code") val itemCode: String? = null,
    @SerializedName("item_name") val itemName: String? = null,
    val category: String? = null,
    val quantity: Double = 0.0,
    @SerializedName("price_per_unit") val pricePerUnit: Double = 0.0,
    val discount: Double = 0.0,
    @SerializedName("total_discount") val totalDiscount: Double = 0.0,
    val amount: Double = 0.0
)

data class DeliveryAgentItemSummary(
    @SerializedName("item_id") val itemId: Int? = null,
    @SerializedName("item_code") val itemCode: String? = null,
    @SerializedName("item_name") val itemName: String? = null,
    val category: String? = null,
    val quantity: Double = 0.0,
    @SerializedName("pcs_count") val pcsCount: Int = 0,
    @SerializedName("bill_count") val billCount: Int = 0,
    @SerializedName("total_amount") val totalAmount: Double = 0.0
)

data class CustomerPaymentHistoryResponse(
    @SerializedName("customer_id") val customerId: Int,
    @SerializedName("customer_name") val customerName: String? = null,
    val month: String? = null,
    val year: String? = null,
    val payments: List<DeliveryCustomerPaymentRecord> = emptyList()
)
