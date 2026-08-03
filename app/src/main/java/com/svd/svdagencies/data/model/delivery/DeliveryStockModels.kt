package com.svd.svdagencies.data.model.delivery

import com.google.gson.annotations.SerializedName

/**
 * Request model for submitting daily stock collection/returns
 */
data class DeliveryStockEntryRequest(
    @SerializedName("delivery_agent") val deliveryAgent: Int,
    val date: String,
    val items: List<StockEntryItemInput>
)

data class StockEntryItemInput(
    @SerializedName("item_id") val itemId: Int,
    val quantity: Double
)

/**
 * Dashboard / Report Response models
 */
data class DeliveryDashboardReportResponse(
    val success: Boolean,
    @SerializedName("delivery_agent") val deliveryAgent: Int,
    @SerializedName("delivery_agent_name") val agentName: String,
    val date: String,
    val summary: DeliveryStockSummary,
    val items: List<DeliveryStockDashboardItem>
)

data class DeliveryStockSummary(
    @SerializedName("morning_taken") val morningStock: Double,
    @SerializedName("morning_return") val morningReturn: Double,
    @SerializedName("evening_taken") val eveningStock: Double,
    @SerializedName("evening_return") val eveningReturn: Double,
    @SerializedName("total_taken") val totalStock: Double,
    @SerializedName("total_return") val totalReturn: Double,
    @SerializedName("total_billed") val totalBilled: Double,
    val difference: Double
) {
    val netStock: Double get() = totalStock - totalReturn
}

data class DeliveryStockDashboardItem(
    @SerializedName("item_id") val itemId: Int,
    @SerializedName("item_code") val itemCode: String?,
    @SerializedName("item_name") val itemName: String,
    val image: String? = null,
    @SerializedName("morning_taken") val morningStock: Double,
    @SerializedName("morning_return") val morningReturn: Double,
    @SerializedName("evening_taken") val eveningStock: Double,
    @SerializedName("evening_return") val eveningReturn: Double,
    @SerializedName("total_taken") val totalStock: Double,
    @SerializedName("total_return") val totalReturn: Double,
    @SerializedName("billed_qty") val billedQty: Double,
    val difference: Double
) {
    val netStock: Double get() = totalStock - totalReturn
}

/**
 * History and Monthly Summary models
 */
data class DeliveryStockHistoryResponse(
    val success: Boolean,
    @SerializedName("delivery_agent") val deliveryAgent: Int,
    val history: List<DeliveryStockHistoryItem>
)

data class DeliveryStockHistoryItem(
    @SerializedName("entry_date") val date: String,
    @SerializedName("morning_stock") val morningStock: Double,
    @SerializedName("morning_return") val morningReturn: Double,
    @SerializedName("evening_stock") val eveningStock: Double,
    @SerializedName("evening_return") val eveningReturn: Double
)

data class DeliveryMonthlySummaryResponse(
    val success: Boolean,
    @SerializedName("delivery_agent") val deliveryAgent: Int,
    val year: Int,
    val month: Int,
    @SerializedName("grand_total") val grandTotal: DeliveryMonthlyGrandTotal,
    val days: List<DeliveryMonthlyDayEntry>
)

data class DeliveryMonthlyGrandTotal(
    @SerializedName("morning_stock") val morningStock: Double,
    @SerializedName("morning_return") val morningReturn: Double,
    @SerializedName("evening_stock") val eveningStock: Double,
    @SerializedName("evening_return") val eveningReturn: Double,
    @SerializedName("total_stock") val totalStock: Double,
    @SerializedName("total_return") val totalReturn: Double,
    @SerializedName("net_stock") val netStock: Double
)

data class DeliveryMonthlyDayEntry(
    val date: String,
    @SerializedName("morning_stock") val morningStock: Double,
    @SerializedName("morning_return") val morningReturn: Double,
    @SerializedName("evening_stock") val eveningStock: Double,
    @SerializedName("evening_return") val eveningReturn: Double,
    @SerializedName("total_stock") val totalStock: Double,
    @SerializedName("total_return") val totalReturn: Double,
    @SerializedName("net_stock") val netStock: Double
)

/**
 * Allowed items for entry
 */
data class DeliveryAllowedItemsResponse(
    val success: Boolean,
    val count: Int,
    val items: List<DeliveryAllowedItem>
)

data class DeliveryAllowedItem(
    @SerializedName("item_id") val itemId: Int,
    val name: String,
    val code: String? = null,
    val image: String? = null,
    @SerializedName("pcs_count") val pcsCount: Int? = 1,
    @SerializedName("category_name") val categoryName: String? = null,
    val quantity: Double? = 0.0
)
