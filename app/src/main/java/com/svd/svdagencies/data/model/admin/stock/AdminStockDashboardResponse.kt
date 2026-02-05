package com.svd.svdagencies.data.model.admin.stock

import com.google.gson.annotations.SerializedName

data class AdminStockDashboardResponse(
    @SerializedName("summary") val summary: StockSummary,
    @SerializedName("all_items") val allItems: List<StockItem>,
    @SerializedName("top_items") val topItems: List<StockItem>,
    @SerializedName("company_data") val companyData: List<CompanyStockValue>
)

data class StockSummary(
    @SerializedName("total_items") val totalItems: Int,
    @SerializedName("total_stock_value") val totalStockValue: Double,
    @SerializedName("low_stock_count") val lowStockCount: Int,
    @SerializedName("stock_in_30d") val stockIn30d: Double,
    @SerializedName("stock_out_30d") val stockOut30d: Double
)

data class StockItem(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("stock_quantity") val stockQuantity: Double,
    @SerializedName("selling_price") val sellingPrice: Double,
    @SerializedName("company_name") val companyName: String,
    @SerializedName("stock_value") val stockValue: Double? = null,
    @SerializedName("pcs_count") val pcsCount: Int? = 1,
)

data class CompanyStockValue(
    @SerializedName("company_name") val companyName: String,
    @SerializedName("total_value") val totalValue: Double
)
