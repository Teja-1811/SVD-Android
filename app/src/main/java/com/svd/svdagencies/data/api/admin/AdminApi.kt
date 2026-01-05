package com.svd.svdagencies.data.api.admin

import com.svd.svdagencies.data.model.admin.AdminDashboardResponse
import retrofit2.Call
import retrofit2.http.GET

interface AdminApi {

    @GET("api/dashboard-counts/")
    fun getDashboardCounts(): Call<AdminDashboardResponse>
}