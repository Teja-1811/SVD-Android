package com.svd.svdagencies.data.model.delivery

import com.google.gson.annotations.SerializedName

data class DeliveryBillItemsResponse(
    @SerializedName("allowed_item_codes") val allowedItemCodes: List<String>,
    val items: List<DeliveryBillItem>
)

data class DeliveryBillCustomer(
    val id: Int,
    val name: String,
    val phone: String? = null,
    val area: String? = null,
    @SerializedName("shop_name") val shopName: String? = null,
    @SerializedName("route_id") val routeId: Int? = null,
    @SerializedName("route_name") val routeName: String? = null,
    @SerializedName("user_type") val userType: String? = "user",
    val due: String? = null
) {
    val label: String
        get() = listOf(name, phone)
            .filter { !it.isNullOrBlank() }
            .joinToString(" - ")
}

data class DeliveryBillCustomersResponse(
    val success: Boolean,
    val count: Int,
    val results: List<DeliveryBillCustomer>
)

data class DeliveryRoute(
    val id: Int,
    val name: String,
    @SerializedName("customers_count") val customersCount: Int
)


data class DeliveryBillItem(
    @SerializedName("item_id") val itemId: Int,
    val code: String,
    val name: String,
    val category: String,
    val price: Double,
    @SerializedName("selling_price") val sellingPrice: Double,
    val mrp: Double,
    @SerializedName("stock_quantity") val stockQuantity: Int,
    @SerializedName("pcs_count") val pcsCount: Int = 0,
    @SerializedName("image_url") val imageUrl: String?
)

data class DeliveryGenerateBillRequest(
    @SerializedName("customer_id") val customerId: Int,
    @SerializedName("bill_date") val billDate: String,
    @SerializedName("bill_items") val items: List<BillLineItem>,
    @SerializedName("paid_amount") val paidAmount: Double = 0.0,
    @SerializedName("payment_method") val paymentMethod: String? = null,
    @SerializedName("transaction_id") val transactionId: String? = null,
    @SerializedName("billing_target") val billingTarget: String? = null,
    @SerializedName("pricing_mode") val pricingMode: String? = null
)

data class BillLineItem(
    @SerializedName("item_id") val itemId: Int,
    val quantity: Int,
    val discount: Double = 0.0
)

data class DeliveryGenerateBillResponse(
    val success: Boolean,
    val message: String,
    val bill: DeliveryBillSummary?
)

data class DeliveryBillSummary(
    @SerializedName("bill_id") val billId: Int,
    @SerializedName("invoice_number") val invoiceNumber: String,
    @SerializedName(value = "invoice_amount", alternate = ["total_amount"])
    val invoiceAmount: Double,
    @SerializedName(value = "op_due", alternate = ["op_due_amount"])
    val openingDue: Double = 0.0,
    @SerializedName(value = "paid", alternate = ["last_paid", "paid_amount"])
    val paid: Double = 0.0,
    @SerializedName("remaining_due") val remainingDue: Double = 0.0,
    @SerializedName("customer_id") val customerId: Int? = null,
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("public_invoice_url") val publicInvoiceUrl: String? = null
) {
    /** Legacy accessor for endpoints that still return total_amount. */
    val totalAmount: Double get() = invoiceAmount
}

data class DeliveryTodayBill(
    @SerializedName("id") val id: Int,
    @SerializedName("bill_id") val billId: Int,
    @SerializedName("invoice_number") val billNumber: String?,
    @SerializedName(value = "invoice_amount", alternate = ["total_amount"])
    val invoiceAmount: Double,
    @SerializedName(value = "op_due", alternate = ["op_due_amount"])
    val openingDue: Double = 0.0,
    @SerializedName(value = "paid", alternate = ["last_paid", "paid_amount"])
    val paid: Double = 0.0,
    @SerializedName("remaining_due") val remainingDue: Double = 0.0,
    @SerializedName("invoice_date") val date: String?,
    @SerializedName("public_invoice_url") val publicInvoiceUrl: String?,
    @SerializedName("customer") val customer: String? = null,
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("customer_phone") val customerPhone: String? = null,
    @SerializedName("customer_shop_name") val customerShopName: String? = null,
    @SerializedName("current_due") val currentDue: Double = 0.0,
    @SerializedName("due_amount") val dueAmount: Double = 0.0,
    val items: List<DeliveryTodayBillItem> = emptyList()
) {
    val realId: Int get() = if (billId > 0) billId else id
    val totalAmount: Double get() = invoiceAmount
}

data class DeliveryTodayBillItem(
    @SerializedName(value = "name", alternate = ["item_name"])
    val name: String? = null,
    val quantity: Double = 0.0,
    @SerializedName("price_per_unit") val pricePerUnit: Double = 0.0,
    val discount: Double = 0.0,
    @SerializedName(value = "total_amount", alternate = ["amount"])
    val totalAmount: Double = 0.0
)

data class DeliveryTodayBillsResponse(
    val bills: List<DeliveryTodayBill>,
    @SerializedName("total_invoice_amount") val totalInvoiceAmount: Double,
)

data class CustomerOpeningBalanceResponse(
    @SerializedName("customer_id") val customerId: Int,
    @SerializedName("customer_name") val customerName: String,
    @SerializedName(value = "opening_balance", alternate = ["op_due", "current_due"])
    val openingBalance: Double
)

data class SaveAgentCollectionResponse(
    val success: Boolean,
    val message: String? = null,
    @SerializedName("customer_id") val customerId: Int? = null,
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("new_due") val newDue: Double? = null
)
