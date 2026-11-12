package com.svd.svdagencies.utils

import retrofit2.Call
import retrofit2.http.*

data class LocationRequestState(val request_id: String, val status: String, val message: String?, val expires_at: String, val requested_at: String)
data class LocationRequestEnvelope(val request: LocationRequestState?)

data class AgentPosition(val id: Int, val latitude: Double?, val longitude: Double?, val recent: Boolean, val name: String, val route: String, val status: String, val updated_at: String?, val maps_url: String?, val accuracy: Double?, val location_request: LocationRequestState? = null)
data class AgentPositions(val agents: List<AgentPosition>)
data class PublicTrackingShare(val url: String, val expires_at: String)
interface LiveLocationApi {
    @POST("api/admin/live-locations/{agent_id}/request/") fun requestLocation(@Path("agent_id") agentId: Int): Call<LocationRequestState>
    @GET("api/delivery/location-requests/") fun pendingLocationRequest(): Call<LocationRequestEnvelope>
    @GET("api/delivery/location-requests/{request_id}/") fun locationRequest(@Path("request_id") requestId: String): Call<LocationRequestEnvelope>
    @POST("api/delivery/location-requests/{request_id}/") fun answerLocationRequest(@Path("request_id") requestId: String, @Body position: Map<String, @JvmSuppressWildcards Any>): Call<Void>
    @POST("api/admin/live-locations/{agent_id}/share/") fun share(@Path("agent_id") agentId: Int): Call<PublicTrackingShare>
    @POST("api/delivery/live-location/") fun send(@Body position: Map<String, @JvmSuppressWildcards Any>): Call<Void>
    @DELETE("api/delivery/live-location/") fun stop(): Call<Void>
    @GET("api/admin/live-locations/") fun agents(): Call<AgentPositions>
}

object LiveLocationNetwork {
    // Location payloads must not pass through the app's BODY logging interceptor.
    val api: LiveLocationApi by lazy {
        val context = com.svd.svdagencies.App.context
        val client = okhttp3.OkHttpClient.Builder()
            .addInterceptor(com.svd.svdagencies.data.api.auth.AuthInterceptor(SessionManager(context)))
            .callTimeout(25, java.util.concurrent.TimeUnit.SECONDS)
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        retrofit2.Retrofit.Builder()
            .baseUrl(com.svd.svdagencies.data.api.auth.ApiClient.BASE_URL)
            .client(client)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build().create(LiveLocationApi::class.java)
    }
}
