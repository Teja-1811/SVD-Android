package com.svd.svdagencies.data.model.admin

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AdminItem(
    val id: Int,
    val code: String?,
    val name: String,
    val company: String?,
    val category: String?,
    val selling_price: String?,
    val buying_price: String?,
    val mrp: String?,
    val stock_quantity: Double?,
    val pcs_count: Int?,
    val image: String?,
    val frozen: Boolean
) : Parcelable
