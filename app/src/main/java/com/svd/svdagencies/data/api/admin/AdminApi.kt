package com.svd.svdagencies.data.api.admin

import com.svd.svdagencies.data.model.admin.AdminDashboardResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers

interface AdminApi {

    @Headers("Cache-Control: no-cache")
    @GET("api/dashboard-counts/")
    fun getDashboardCounts(): Call<AdminDashboardResponse>
}