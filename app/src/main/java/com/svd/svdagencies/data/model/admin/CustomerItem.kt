package com.svd.svdagencies.data.model.admin

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CustomerItem(
    val id: Int? = 0,
    val serial_no: Int? = 0,
    val name: String? = "",
    val shop_name: String? = "",
    val phone: String? = "",
    val due: Double? = 0.0,
    val frozen: Boolean? = false
) : Parcelable
