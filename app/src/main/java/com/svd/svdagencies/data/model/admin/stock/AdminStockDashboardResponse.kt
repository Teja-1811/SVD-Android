package com.svd.svdagencies.data.model.admin.stock

import com.google.gson.annotations.SerializedName

data class AdminStockDashboardResponse(
    @SerializedName("summary") val summary: StockSummary,
    @SerializedName("selected_date") val selectedDate: String? = null,
    @SerializedName("date_entries") val dateEntries: List<StockDateEntry> = emptyList(),
    @SerializedName("company_totals") val companyTotals: List<StockCompanyTotal> = emptyList(),
    @SerializedName("leakage_entries") val leakageEntries: List<StockLeakageEntry> = emptyList(),
    @SerializedName("all_items") val allItems: List<StockItem>,
    @SerializedName("top_items") val topItems: List<StockItem>,
    @SerializedName("company_data") val companyData: List<CompanyStockValue>
)

data class StockSummary(
    @SerializedName("total_items") val totalItems: Int,
    @SerializedName("total_stock_value") val totalStockValue: Double,
    @SerializedName("low_stock_count") val lowStockCount: Int,
    @SerializedName("stock_in_30d") val stockIn30d: Double,
    @SerializedName("stock_out_30d") val stockOut30d: Double,
    @SerializedName("monthly_loss") val monthlyLoss: Double = 0.0,
    @SerializedName("entries_on_date") val entriesOnDate: Int = 0,
    @SerializedName("day_total_value") val dayTotalValue: Double = 0.0
)

data class StockItem(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("stock_quantity") val stockQuantity: Double,
    @SerializedName("selling_price") val sellingPrice: Double = 0.0,
    @SerializedName("buying_price") val buyingPrice: Double = 0.0,
    @SerializedName("company_name") val companyName: String? = "Unknown",
    @SerializedName("stock_value") val stockValue: Double? = null,
    @SerializedName("pcs_count") val pcsCount: Int? = 1,
    @SerializedName("category_name") val categoryName: String? = null,
    @SerializedName("item_code") val itemCode: String? = null,
    @SerializedName("image") val image: String? = null
)

data class CompanyStockValue(
    @SerializedName("company_name") val companyName: String,
    @SerializedName("total_value") val totalValue: Double
)

data class StockDateEntry(
    @SerializedName("id") val id: Int,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("company_name") val companyName: String? = null,
    @SerializedName("item_name") val itemName: String,
    @SerializedName("crates") val crates: Double,
    @SerializedName("quantity") val quantity: Double,
    @SerializedName("value") val value: Double
)

data class StockCompanyTotal(
    @SerializedName("company_name") val companyName: String,
    @SerializedName("total_crates") val totalCrates: Double,
    @SerializedName("total_quantity") val totalQuantity: Double,
    @SerializedName("total_value") val totalValue: Double
)

data class StockLeakageEntry(
    @SerializedName("id") val id: Int,
    @SerializedName("date") val date: String,
    @SerializedName("item_name") val itemName: String? = null,
    @SerializedName("company_name") val companyName: String? = null,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("unit_cost") val unitCost: Double,
    @SerializedName("total_loss") val totalLoss: Double,
    @SerializedName("notes") val notes: String? = null
)
