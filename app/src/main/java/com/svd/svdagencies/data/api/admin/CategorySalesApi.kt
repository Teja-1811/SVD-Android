package com.svd.svdagencies.data.api.admin

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

data class CategorySalesResponse(
    val period: String,
    val categories: List<String>,
    val data_by_category: Map<String, Double>,
    val amount_by_category: Map<String, Double>,
    val total_volume: Double,
)

interface CategorySalesApi {
    @GET("api/sales/category-summary/")
    fun getSummary(
        @Query("period") period: String = "overall",
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
    ): Call<CategorySalesResponse>
}
