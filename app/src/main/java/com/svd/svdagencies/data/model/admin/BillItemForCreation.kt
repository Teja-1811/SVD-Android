package com.svd.svdagencies.data.model.admin

/**
 * Represents a single item row in the CreateBillActivity's RecyclerView.
 * This is a UI-specific model to hold the state before the bill is created.
 */
data class BillItemForCreation(
    var itemId: Int = 0,
    var quantity: Int = 1,
    var discount: Double = 0.0
)
