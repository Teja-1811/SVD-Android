package com.svd.svdagencies.data.api.user

import com.svd.svdagencies.data.model.user.UserDashboardResponse
import com.svd.svdagencies.data.model.user.UserOffersResponse
import com.svd.svdagencies.data.model.user.UserPlansResponse
import com.svd.svdagencies.data.model.user.UserProfileUpdateResponse
import com.svd.svdagencies.data.model.user.UserSubscription
import com.svd.svdagencies.data.model.user.PendingOrdersResponse
import com.svd.svdagencies.data.model.user.OrderActionResponse
import com.svd.svdagencies.data.model.user.CreateOrderRequest
import com.svd.svdagencies.data.model.user.UserBillsResponse
import com.svd.svdagencies.data.model.user.UserBillDetailResponse
import com.svd.svdagencies.data.model.user.UserOrderDetailResponse
import kotlin.jvm.JvmSuppressWildcards
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserApi {
    @GET("api/user/dashboard/")
    fun getDashboard(): Call<UserDashboardResponse>

    @GET("api/user/offers/")
    fun getActiveOffers(): Call<UserOffersResponse>

    @GET("api/user/subscriptions/plans/")
    fun getAvailablePlans(): Call<UserPlansResponse>

    @POST("api/user/profile/")
    fun updateProfile(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Call<UserProfileUpdateResponse>

    @POST("api/user/subscriptions/pause-resume/")
    fun pauseResumeSubscription(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Call<Map<String, Any>>

    @POST("api/user/orders/create/")
    fun prebookOrder(
        @Body request: CreateOrderRequest
    ): Call<OrderActionResponse>

    @GET("api/user/subscriptions/current/")
    fun getCurrentSubscription(): Call<UserSubscription>

    @GET("api/user/orders/pending/")
    suspend fun getPendingOrders(): PendingOrdersResponse

    @GET("api/user/bills/")
    fun getUserBills(): Call<UserBillsResponse>

    @GET("api/user/bills/{bill_id}/")
    fun getUserBillDetail(
        @Path("bill_id") billId: Int
    ): Call<UserBillDetailResponse>

    @GET("api/user/orders/{order_id}/")
    fun getUserOrderDetail(
        @Path("order_id") orderId: Int
    ): Call<UserOrderDetailResponse>

    @POST("api/user/orders/create/")
    suspend fun createOrder(
        @Body request: CreateOrderRequest
    ): OrderActionResponse

    @PUT("api/user/orders/{order_id}/edit/")
    suspend fun editOrder(
        @Path("order_id") orderId: Int,
        @Body request: CreateOrderRequest
    ): OrderActionResponse

    @DELETE("api/user/orders/{order_id}/delete/")
    suspend fun deleteOrder(
        @Path("order_id") orderId: Int
    ): Map<String, Any>
}
