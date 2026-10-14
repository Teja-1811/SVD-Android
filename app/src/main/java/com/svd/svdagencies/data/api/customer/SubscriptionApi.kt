package com.svd.svdagencies.data.api.customer

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SubscriptionApi {
    @GET("api/customer/subscription-offers/")
    fun getSubscriptionOffers(): Call<JsonObject>
    @GET("api/customer/subscriptions/")
    fun getSubscriptions(): Call<JsonObject>

    @POST("api/customer/subscriptions/")
    fun createSubscription(@Body request: JsonObject): Call<JsonObject>

    @POST("api/customer/subscriptions/{id}/pause/")
    fun pauseSubscription(@Path("id") id: Int, @Body request: JsonObject): Call<JsonObject>

    @POST("api/customer/subscriptions/{id}/resume/")
    fun resumeSubscription(@Path("id") id: Int, @Body request: JsonObject): Call<JsonObject>

    @POST("api/customer/subscriptions/{id}/cancel/")
    fun cancelSubscription(@Path("id") id: Int, @Body request: JsonObject): Call<JsonObject>
}
