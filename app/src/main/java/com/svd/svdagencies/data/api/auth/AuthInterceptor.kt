package com.svd.svdagencies.data.api.auth

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import com.svd.svdagencies.utils.SessionManager
import okio.Buffer

class AuthInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        val builder = originalRequest.newBuilder()

        if (path.contains("/api/auth/login")) {
            // Force Content-Type to application/json WITHOUT charset
            // Some Django parsers are strict about this.
            builder.header("Content-Type", "application/json")
            builder.header("Accept", "application/json")
            return chain.proceed(builder.build())
        }

        builder.header("Accept", "application/json")
        val token = sessionManager.getToken()
        if (!token.isNullOrEmpty()) {
            builder.header("Authorization", "Token $token")
        }

        return chain.proceed(builder.build())
    }
}
