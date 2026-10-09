package com.svd.svdagencies.data.api.admin

import com.svd.svdagencies.data.model.admin.customerData.CustomerItemDiscountDashboardResponse
import com.svd.svdagencies.data.model.admin.customerData.SaveCustomerItemDiscountsRequest
import com.svd.svdagencies.data.model.admin.customerData.SaveCustomerItemDiscountsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CustomerItemDiscountApi {
    @GET("api/customer-item-discounts/")
    suspend fun getDashboard(
        @Query("customer_id") customerId: Int? = null
    ): CustomerItemDiscountDashboardResponse

    @POST("api/customer-item-discounts/")
    suspend fun saveDiscounts(
        @Body request: SaveCustomerItemDiscountsRequest
    ): SaveCustomerItemDiscountsResponse
}
