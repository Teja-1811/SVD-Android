package com.svd.svdagencies.data.model.admin

/**
 * Request body for editing a bill.
 * Unlike creating, customer_id is not needed as it's part of the URL.
 */
data class EditBillRequest(
    val items: List<Int>,
    val quantities: List<Int>,
    val discounts: List<Double>
)
