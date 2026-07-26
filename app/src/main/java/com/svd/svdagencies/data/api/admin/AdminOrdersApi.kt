package com.svd.svdagencies.data.api.admin

import com.svd.svdagencies.data.model.admin.*
import com.svd.svdagencies.data.model.admin.Orders.AdminOrderDetail
import com.svd.svdagencies.data.model.admin.Orders.ConfirmOrderRequest
import com.svd.svdagencies.data.model.admin.Orders.ConfirmOrderResponse
import com.svd.svdagencies.data.model.admin.Orders.OrdersDashboardResponse
import retrofit2.Response
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
    ): Response<ConfirmOrderResponse>

    @POST("api/orders/{order_id}/cancel/")
    suspend fun rejectOrder(@Path("order_id") orderId: Int): Response<Map<String, Any>>
}
