package com.svd.svdagencies.data.model.admin

data class CreateBillRequest(
    val customer: Int,
    val items: List<Int>,
    val quantities: List<Int>,
    val discounts: List<Double>
)
