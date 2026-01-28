package com.svd.svdagencies.data.model.admin.Bills

data class BillCustomer(
    val id: Int,
    val name: String,
    val phone: String,
    val area: String,
    val due: String
)