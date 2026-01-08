package com.svd.svdagencies.data.model.admin

data class BillListResponse(
    val results: List<BillItem>,
    val current_page: Int,
    val total_pages: Int,
    val total_records: Int
)
