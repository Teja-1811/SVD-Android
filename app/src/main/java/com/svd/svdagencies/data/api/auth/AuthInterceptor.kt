package com.svd.svdagencies.data.api.auth

import android.util.Log
import com.svd.svdagencies.utils.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessionManager.getToken()

        if (token == null) {
            Log.e("AuthInterceptor", "Token is NULL. Request will fail if auth is required.")
        } else {
            Log.d("AuthInterceptor", "Token found: $token")
        }

        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Token $token")
                .build()
        } else {
            chain.request()
        }

        return chain.proceed(request)
    }
}
