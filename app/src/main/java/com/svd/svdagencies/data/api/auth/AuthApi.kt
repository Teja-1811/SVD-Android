package com.svd.svdagencies.data.api.auth

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    // Note: No leading slash here because BASE_URL has a trailing slash.
    // Ensure this matches your Django urls.py exactly (including the trailing slash).
    @POST("api/auth/login/")
    fun login(
        @Body request: LoginRequest
    ): Call<LoginResponse>

    @POST("api/auth/register/")
    fun register(
        @Body request: RegisterRequest
    ): Call<RegistrationResponse>
}
