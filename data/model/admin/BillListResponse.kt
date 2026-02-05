package com.svd.svdagencies.data.model.admin

import com.svd.svdagencies.data.model.admin.Bills.AdminBill

data class BillListResponse(
    val results: List<AdminBill>,
    val current_page: Int,
    val total_pages: Int,
    val total_records: Int
)
