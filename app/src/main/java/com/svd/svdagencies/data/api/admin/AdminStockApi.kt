package com.svd.svdagencies.data.api.admin

import com.svd.svdagencies.data.model.admin.stock.AdminStockDashboardResponse
import retrofit2.Call
import retrofit2.http.*

interface AdminStockApi {

    @GET("api/stock/dashboard/")
    fun getStockDashboard(
        @Query("date") date: String? = null,
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null
    ): Call<AdminStockDashboardResponse>

    @POST("api/stock/update/")
    fun updateStock(@Body body: @JvmSuppressWildcards Map<String, Any>): Call<Map<String, Any>>

    @PATCH("api/stock/entries/{id}/edit/")
    fun editStockEntry(
        @Path("id") id: Int,
        @Body body: @JvmSuppressWildcards Map<String, Any>
    ): Call<Map<String, Any>>

    @POST("api/stock/entries/{id}/delete/")
    fun deleteStockEntry(
        @Path("id") id: Int
    ): Call<Map<String, Any>>

    @POST("api/stock/leakage/save/")
    fun saveLeakage(
        @Body body: @JvmSuppressWildcards Map<String, Any>
    ): Call<Map<String, Any>>

    @POST("api/stock/leakage/{id}/delete/")
    fun deleteLeakage(
        @Path("id") id: Int
    ): Call<Map<String, Any>>
}
