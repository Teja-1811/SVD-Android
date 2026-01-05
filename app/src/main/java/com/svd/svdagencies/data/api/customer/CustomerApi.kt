package com.svd.svdagencies.data.api.customer

import com.svd.svdagencies.data.model.customer.CustomerDashboardResponse
import com.svd.svdagencies.data.model.customer.GenericResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CustomerApi {
    @GET("api/customer-dashboard/")
    fun getDashboard(
        @Query("user_id") userId: Int
    ): Call<CustomerDashboardResponse>

    @POST("api/customer/payment/")
    fun recordCustomerPayment(
        @Body body: Map<String, String>
    ): Call<GenericResponse>

}