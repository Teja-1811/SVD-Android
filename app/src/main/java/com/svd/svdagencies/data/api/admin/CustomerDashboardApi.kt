package com.svd.svdagencies.data.api.admin

import com.svd.svdagencies.data.model.admin.CustomerDashboardResponse
import com.svd.svdagencies.data.model.admin.CustomerDetail
import com.svd.svdagencies.data.model.admin.ToggleFreezeResponse
import com.svd.svdagencies.data.model.admin.UpdateBalanceRequest
import com.svd.svdagencies.data.model.admin.UpdateBalanceResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface CustomerDashboardApi {

    // Changed endpoint from api/customer-list/ to api/customers/ as a potential fix
    @GET("api/customers/")
    fun getCustomers(@Header("Authorization") token: String): Call<CustomerDashboardResponse>

    @GET("api/customers/{id}/")
    fun getCustomerDetail(@Path("id") id: Int): Call<CustomerDetail>

    @POST("api/customers/{id}/freeze/")
    fun toggleFreeze(@Path("id") id: Int): Call<ToggleFreezeResponse>

    @POST("api/customers/{id}/balance/")
    fun updateBalance(@Path("id") id: Int, @Body request: UpdateBalanceRequest): Call<UpdateBalanceResponse>

}
