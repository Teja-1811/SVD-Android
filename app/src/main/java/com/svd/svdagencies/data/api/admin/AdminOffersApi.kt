package com.svd.svdagencies.data.api.admin

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import com.google.gson.JsonObject

data class AdminOffer(val id: Int, val name: String, val description: String = "", val price: Double = 0.0, val offer_for: String = "retailer", val offer_type: String = "bundle", val start_date: String = "", val end_date: String = "", val is_active: Boolean = true)
data class AdminOffersResponse(val offers: List<AdminOffer>)

interface AdminOffersApi {
    @GET("api/offers/dashboard/") fun listOffers(): Call<AdminOffersResponse>
    @POST("api/offers/create/") fun createOffer(@Body body: Map<String, Any>): Call<Map<String, Any>>
    @POST("api/offers/update/{id}/") fun updateOffer(@Path("id") id: Int, @Body body: Map<String, Any>): Call<Map<String, Any>>
    @DELETE("api/offers/delete/{id}/") fun deleteOffer(@Path("id") id: Int): Call<Map<String, Any>>
    @POST("api/offers/item/add/{id}/") fun addOfferItem(@Path("id") id: Int, @Body body: Map<String, Any>): Call<Map<String, Any>>
    @GET("api/subscription-offers/") fun listSubscriptionOffers(): Call<JsonObject>
    @POST("api/subscription-offers/") fun createSubscriptionOffer(@Body body: Map<String, Any>): Call<JsonObject>
    @POST("api/subscription-offers/{id}/") fun updateSubscriptionOffer(@Path("id") id: Int, @Body body: Map<String, Any>): Call<JsonObject>
}
