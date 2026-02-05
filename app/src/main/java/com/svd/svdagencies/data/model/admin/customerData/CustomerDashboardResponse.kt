package com.svd.svdagencies.data.model.admin.customerData

import com.svd.svdagencies.data.model.admin.customerData.CustomerItem

data class CustomerDashboardResponse(
    val customers: List<CustomerItem>? = emptyList()
)