package com.svd.svdagencies.data.api.admin

import com.svd.svdagencies.data.model.admin.*
import retrofit2.Call
import retrofit2.http.*

interface SubscriptionApi {

    @GET("api/subscriptions/dashboard/")
    fun getSubscriptionDashboard(): Call<SubscriptionDashboardResponse>

    @GET("api/subscriptions/plans/")
    fun getPlans(): Call<List<SubscriptionPlan>>

    @POST("api/subscriptions/create-plan/")
    fun createPlan(@Body planData: @JvmSuppressWildcards Map<String, Any>): Call<Map<String, Any>>

    @POST("api/subscriptions/edit-plan/{id}/")
    fun updatePlan(@Path("id") id: Int, @Body planData: @JvmSuppressWildcards Map<String, Any>): Call<Map<String, Any>>

    @POST("api/subscriptions/plan/{id}/add-item/")
    fun addItemToPlan(@Path("id") id: Int, @Body itemData: @JvmSuppressWildcards Map<String, Any>): Call<Map<String, Any>>

    @POST("api/subscriptions/item/{id}/update/")
    fun updatePlanItem(@Path("id") id: Int, @Body itemData: @JvmSuppressWildcards Map<String, Any>): Call<Map<String, Any>>

    @DELETE("api/subscriptions/item/{id}/delete/")
    fun deletePlanItem(@Path("id") id: Int): Call<Void>

    @GET("api/subscriptions/customers/")
    fun getSubscriptionCustomers(): Call<List<SubscriptionCustomer>>

    @POST("api/subscriptions/assign/")
    fun assignSubscription(@Body assignData: @JvmSuppressWildcards Map<String, Any>): Call<Map<String, Any>>

    @GET("api/subscriptions/list/")
    fun getCustomerSubscriptions(@Query("customer") customerId: Int? = null): Call<List<CustomerSubscription>>

    @GET("api/subscriptions/history/")
    fun getSubscriptionHistory(
        @Query("customer") customerId: Int? = null,
        @Query("plan") planId: Int? = null
    ): Call<Map<String, Any>>

    @POST("api/subscriptions/toggle/{id}/")
    fun toggleSubscription(@Path("id") subscriptionId: Int): Call<Map<String, Any>>

    @POST("api/subscriptions/payment/{id}/")
    fun recordPayment(@Path("id") subscriptionId: Int, @Body paymentData: @JvmSuppressWildcards Map<String, Any>): Call<Map<String, Any>>

    @GET("api/subscriptions/today-deliveries/")
    fun getTodayDeliveries(): Call<List<Delivery>>
}
