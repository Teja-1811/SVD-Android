package com.svd.svdagencies.data.api.admin

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class AdminUser(
    val id: Int,
    val name: String,
    val phone: String = "",
    val shop_name: String = "",
    val due: Double = 0.0,
    val frozen: Boolean = false,
)

data class AdminUsersResponse(val users: List<AdminUser>)

interface AdminUsersApi {
    @GET("api/users/") fun listUsers(): Call<AdminUsersResponse>
    @POST("api/users/add/") fun saveUser(@Body body: Map<String, String>): Call<Map<String, Any>>
    @POST("api/users/freeze/{id}/") fun toggleFreeze(@Path("id") id: Int): Call<Map<String, Any>>
}
