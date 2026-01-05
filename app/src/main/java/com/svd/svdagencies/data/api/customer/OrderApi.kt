package com.svd.svdagencies.data.api.customer

import com.svd.svdagencies.data.model.customer.PlaceOrderRequest
import com.svd.svdagencies.data.model.customer.PlaceOrderResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface OrderApi {

    @POST("api/place-order/")
    fun placeOrder(
        @Body request: PlaceOrderRequest
    ): Call<PlaceOrderResponse>
}