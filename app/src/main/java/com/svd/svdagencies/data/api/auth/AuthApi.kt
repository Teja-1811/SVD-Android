package com.svd.svdagencies.data.api.auth

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("api/auth/login/")
    fun login(
        @Body request: LoginRequest
    ): Call<LoginResponse>
}