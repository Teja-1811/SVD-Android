package com.svd.svdagencies.data.api.admin

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

data class AdminRoute(val id: Int, val name: String, val customer_count: Int = 0)
data class AdminRoutesResponse(val routes: List<AdminRoute>)

interface AdminRoutesApi {
    @GET("api/routes/")
    fun listRoutes(): Call<AdminRoutesResponse>

    @POST("api/routes/")
    fun addRoute(@Body body: Map<String, String>): Call<Map<String, Any>>

    @PUT("api/routes/{id}/")
    fun updateRoute(@Path("id") id: Int, @Body body: Map<String, String>): Call<Map<String, Any>>

    @DELETE("api/routes/{id}/")
    fun deleteRoute(@Path("id") id: Int): Call<Map<String, Any>>
}
