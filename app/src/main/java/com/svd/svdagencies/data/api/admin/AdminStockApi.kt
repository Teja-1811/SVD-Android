package com.svd.svdagencies.data.api.admin

import com.svd.svdagencies.data.model.admin.stock.AdminStockDashboardResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body

interface AdminStockApi {

    @GET("api/stock/dashboard/")
    fun getStockDashboard(): Call<AdminStockDashboardResponse>

    @POST("api/stock/update/")
    fun updateStock(@Body body: @JvmSuppressWildcards Map<String, List<Map<String, Any>>>): Call<Map<String, Any>>
}
