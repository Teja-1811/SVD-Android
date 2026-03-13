package com.svd.svdagencies.data.api.customer

import com.svd.svdagencies.data.model.customer.PlaceOrderRequest
import com.svd.svdagencies.data.model.customer.PlaceOrderResponse
import com.svd.svdagencies.data.model.customer.CurrentDayOrdersResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface OrderApi {

    @GET("api/current-day-orders/")
    fun getCurrentDayOrders(): Call<CurrentDayOrdersResponse>

    @POST("api/place-order/")
    fun placeOrder(
        @Body request: PlaceOrderRequest
    ): Call<PlaceOrderResponse>
}
