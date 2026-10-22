package com.svd.svdagencies.data.api.auth

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.svd.svdagencies.App
import com.svd.svdagencies.ui.auth.LoginActivity
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

        if (path.contains("/api/auth/login") || path.contains("/api/auth/register")) {
            // Force Content-Type to application/json WITHOUT charset
            // Some Django parsers are strict about this.
            builder.header("Content-Type", "application/json")
            builder.header("Accept", "application/json")
            return chain.proceed(builder.build())
        }

        builder.header("Accept", "application/json")
        val token = sessionManager.getToken()
        if (originalRequest.header("Authorization") == null && !token.isNullOrEmpty()) {
            builder.header("Authorization", "Token $token")
        }
        val request = builder.build()
        val response = chain.proceed(request)
        val sentToken = request.header("Authorization")?.removePrefix("Token ")
        if (response.code == 401 && !sentToken.isNullOrBlank() && sessionManager.invalidateIfTokenMatches(sentToken)) {
            Handler(Looper.getMainLooper()).post {
                if (sessionManager.getToken().isNullOrBlank()) {
                    Toast.makeText(App.context, "Your session has ended. Please sign in again.", Toast.LENGTH_LONG).show()
                    App.context.startActivity(Intent(App.context, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
            }
        }
        return response
    }
}
