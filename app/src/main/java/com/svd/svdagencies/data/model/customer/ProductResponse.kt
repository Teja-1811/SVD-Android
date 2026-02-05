package com.svd.svdagencies.data.model.customer

data class ProductResponse(
    val id: Int,
    val name: String,
    val company: String,
    val mrp: Double,
    val selling_price: Double,
    val margin: Double,
    val stock: Int,
    val image: String,
    val pcs_count: Int
) {
    val pcs: Int
        get() = if (pcs_count > 0) pcs_count else 1

    val calculateStep: Double
        get() = 1.0

    fun calculateTotal(quantity: Double): Double {
        return quantity * selling_price
    }

    fun formatQuantity(quantity: Double): String {
        return if (quantity == quantity.toInt().toDouble()) {
            quantity.toInt().toString()
        } else {
            quantity.toString()
        }
    }
}