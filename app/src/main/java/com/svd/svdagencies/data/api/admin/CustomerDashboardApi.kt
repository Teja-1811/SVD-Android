package com.svd.svdagencies.data.api.admin

import com.svd.svdagencies.data.model.admin.*
import retrofit2.http.*
import retrofit2.Call

interface CustomerDashboardApi {

    // Get all customers
    @GET("api/customer-list/")
    fun getCustomers(): Call<CustomerDashboardResponse>


    // Get single customer details
    @GET("api/customer-detail/{id}/")
    suspend fun getCustomerDetail(
        @Path("id") id: Int
    ): CustomerDetail


    // Freeze / Unfreeze customer
    @POST("api/customer-freeze/{id}/freeze/")
    suspend fun toggleFreeze(
        @Path("id") id: Int
    ): ToggleFreezeResponse


    // Update balance
    @POST("api/customer-balance/{id}/balance/")
    suspend fun updateBalance(
        @Path("id") id: Int,
        @Body request: UpdateBalanceRequest
    ): UpdateBalanceResponse

    // Add or Edit Customer
    @POST("api/customer-add/")
    suspend fun addOrUpdateCustomer(
        @Body request: AddCustomerRequest
    ): AddCustomerResponse
}
