package com.svd.svdagencies.data.api.admin

import com.svd.svdagencies.data.model.admin.*
import com.svd.svdagencies.data.model.admin.customerData.AddCustomerRequest
import com.svd.svdagencies.data.model.admin.customerData.AddCustomerResponse
import com.svd.svdagencies.data.model.admin.customerData.CustomerDashboardResponse
import com.svd.svdagencies.data.model.admin.customerData.CustomerDetail
import com.svd.svdagencies.data.model.admin.customerData.MonthlySummaryResponse
import com.svd.svdagencies.data.model.admin.customerData.ToggleFreezeResponse
import com.svd.svdagencies.data.model.admin.customerData.UpdateBalanceRequest
import com.svd.svdagencies.data.model.admin.Orders.UpdateBalanceResponse
import okhttp3.ResponseBody
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


    // Freeze / Unfreeze customer - Updated URL based on Server Logs
    @POST("api/customer-freeze/{id}/")
    suspend fun toggleFreeze(
        @Path("id") id: Int
    ): ToggleFreezeResponse


    // Update balance
    @POST("api/customer-balance/{id}/")
    suspend fun updateBalance(
        @Path("id") id: Int,
        @Body request: UpdateBalanceRequest
    ): UpdateBalanceResponse

    // Add or Edit Customer
    @POST("api/customer-add/")
    suspend fun addOrUpdateCustomer(
        @Body request: AddCustomerRequest
    ): AddCustomerResponse

    @GET("api/sales/monthly-summary/")
    suspend fun getMonthlySalesSummary(
        @Query("date") date: String,
        @Query("customer_id") customerId: Int?
    ): MonthlySummaryResponse

    @GET("api/sales/monthly-summary/pdf/")
    suspend fun downloadMonthlySalesPdf(
        @Query("date") date: String,
        @Query("customer") customerId: Int,
        @Query("area") area: String = ""
    ): ResponseBody
}
