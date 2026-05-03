package com.svd.svdagencies.data.api.delivery

import com.svd.svdagencies.data.model.delivery.DeliveryTodayResponse
import com.svd.svdagencies.data.model.delivery.DeliveryUpdateRequest
import com.svd.svdagencies.data.model.delivery.DeliveryUpdateResponse
import com.svd.svdagencies.data.model.delivery.DeliveryBillItemsResponse
import com.svd.svdagencies.data.model.delivery.DeliveryGenerateBillRequest
import com.svd.svdagencies.data.model.delivery.DeliveryGenerateBillResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT

interface DeliveryApi {

    @GET("api/delivery/today/")
    fun getTodayDeliveries(): Call<DeliveryTodayResponse>

    @PATCH("api/delivery/update/")
    fun updateDelivery(@Body request: DeliveryUpdateRequest): Call<DeliveryUpdateResponse>

    @PUT("api/delivery/update/")
    fun updateDeliveryPut(@Body request: DeliveryUpdateRequest): Call<DeliveryUpdateResponse>

    @GET("api/delivery/bill-items/")
    fun getBillItems(@retrofit2.http.Query("customer_id") customerId: Int): Call<DeliveryBillItemsResponse>

    @retrofit2.http.POST("api/delivery/generate-bill/")
    fun generateBill(@Body request: DeliveryGenerateBillRequest): Call<DeliveryGenerateBillResponse>
}
