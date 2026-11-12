package com.svd.svdagencies.data.api.customer

import com.svd.svdagencies.data.model.customer.PlaceOrderRequest
import com.svd.svdagencies.data.model.customer.PlaceOrderResponse
import com.svd.svdagencies.data.model.customer.CurrentDayOrdersResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface OrderApi {

    @GET("api/current-day-orders/")
    fun getCurrentDayOrders(@Query("delivery_date") deliveryDate: String? = null): Call<CurrentDayOrdersResponse>

    @POST("api/place-order/")
    fun placeOrder(
        @Body request: PlaceOrderRequest
    ): Call<PlaceOrderResponse>

    @POST("api/customer/orders/{orderId}/cancel/")
    fun cancelOrder(@Path("orderId") orderId: Int): Call<Map<String, Any>>
}
