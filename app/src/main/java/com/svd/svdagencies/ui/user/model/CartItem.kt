package com.svd.svdagencies.ui.user.model

import com.svd.svdagencies.data.model.admin.Items.AdminItem

data class CartItem(
    val item: AdminItem,
    val quantity: Int
) {
    fun unitPrice(): Double {
        val selling = item.sellingPriceValue
        val mrp = item.mrpValue
        return if (selling > 0) selling else mrp
    }
}
