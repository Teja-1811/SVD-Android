package com.svd.svdagencies.data.model.admin.customerData

data class CustomerDiscountCustomer(
    val id: Int,
    val name: String,
    val shop_name: String = "",
    val user_type: String = "retailer"
)

data class CustomerItemDiscountRow(
    val id: Int,
    val name: String,
    val code: String = "",
    val selling_price: Double = 0.0,
    val mrp: Double = 0.0,
    var discount_per_unit: Double = 0.0
)

data class CustomerItemDiscountDashboardResponse(
    val customers: List<CustomerDiscountCustomer> = emptyList(),
    val items: List<CustomerItemDiscountRow> = emptyList()
)

data class CustomerItemDiscountValue(
    val item_id: Int,
    val discount_per_unit: Double
)

data class SaveCustomerItemDiscountsRequest(
    val customer_id: Int,
    val discounts: List<CustomerItemDiscountValue>
)

data class SaveCustomerItemDiscountsResponse(
    val success: Boolean,
    val message: String? = null
)
