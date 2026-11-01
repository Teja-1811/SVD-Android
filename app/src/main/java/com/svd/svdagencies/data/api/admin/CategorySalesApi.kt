package com.svd.svdagencies.data.api.admin

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

data class CategorySalesResponse(
    val period: String,
    val start_date: String? = null,
    val end_date: String? = null,
    val compare_data_by_category: Map<String,Double> = emptyMap(),
    val total_compare_volume: Double = 0.0,
    val diff_volume: Double = 0.0,
    val diff_percent: Double? = null,
    val trend_by_category: Map<String,List<CategoryVolumePoint>> = emptyMap(),
    val year_totals: Map<String,Double> = emptyMap(),
    val customers: List<CategorySalesCustomer> = emptyList(),
    val categories: List<String>,
    val data_by_category: Map<String, Double>,
    val amount_by_category: Map<String, Double>,
    val total_volume: Double,
)

data class CategoryVolumePoint(val date:String,val volume:Double)
data class CategorySalesCustomer(val id:Int,val name:String)

interface CategorySalesApi {
    @GET("api/sales/category-summary/")
    fun getSummary(
        @Query("period") period: String = "overall",
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("customer") customer: Int? = null,
    ): Call<CategorySalesResponse>
}
