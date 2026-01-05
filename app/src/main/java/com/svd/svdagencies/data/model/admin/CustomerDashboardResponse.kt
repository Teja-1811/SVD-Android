package com.svd.svdagencies.data.model.admin

data class CustomerDashboardResponse(
    val customers: List<CustomerItem> = emptyList()
)
