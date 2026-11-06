package com.svd.svdagencies.utils

import retrofit2.Call
import retrofit2.http.*

data class AgentPosition(val id: Int, val latitude: Double?, val longitude: Double?, val recent: Boolean, val name: String, val route: String, val status: String, val updated_at: String?, val maps_url: String?, val accuracy: Double?)
data class AgentPositions(val agents: List<AgentPosition>)
data class PublicTrackingShare(val url: String, val expires_at: String)
interface LiveLocationApi {
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
