package com.svd.svdagencies.data.api.admin

import com.svd.svdagencies.data.model.admin.*
import retrofit2.http.*

interface AdminOrdersApi {

    @GET("orders/dashboard/")
    suspend fun getOrdersDashboard(): OrdersDashboardResponse

    @GET("orders/{order_id}/detail/")
    suspend fun getOrderDetail(@Path("order_id") orderId: Int): AdminOrderDetail

    @POST("orders/{order_id}/confirm/")
    suspend fun confirmOrder(
        @Path("order_id") orderId: Int,
        @Body request: ConfirmOrderRequest
    ): ConfirmOrderResponse

    @POST("orders/{order_id}/cancel/")
    suspend fun rejectOrder(@Path("order_id") orderId: Int): Map<String, Any>
}
