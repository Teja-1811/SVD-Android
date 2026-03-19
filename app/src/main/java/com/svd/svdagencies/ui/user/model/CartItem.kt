package com.svd.svdagencies.ui.user.model

import com.svd.svdagencies.data.model.admin.Items.AdminItem

data class CartItem(
    val item: AdminItem,
    val quantity: Int
) {
    fun unitPrice(): Double {
        val mrp = item.mrpValue
        val selling = item.sellingPriceValue
        return if (mrp > 0) mrp else selling
    }
}
