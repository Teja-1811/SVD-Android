package com.svd.svdagencies.data.api.admin

import com.svd.svdagencies.data.model.admin.*
import retrofit2.http.*

interface AdminOrdersApi {

    @GET("api/orders/dashboard/")
    suspend fun getOrdersDashboard(): OrdersDashboardResponse

    @GET("api/orders/{order_id}/detail/")
    suspend fun getOrderDetail(@Path("order_id") orderId: Int): AdminOrderDetail

    @POST("api/orders/{order_id}/confirm/")
    suspend fun confirmOrder(
        @Path("order_id") orderId: Int,
        @Body request: ConfirmOrderRequest
    ): ConfirmOrderResponse

    @POST("api/orders/{order_id}/cancel/")
    suspend fun rejectOrder(@Path("order_id") orderId: Int): Map<String, Any>
}
