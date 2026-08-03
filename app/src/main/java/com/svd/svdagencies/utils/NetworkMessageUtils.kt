package com.svd.svdagencies.utils

import org.json.JSONObject
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object NetworkMessageUtils {

    fun friendlyMessage(throwable: Throwable?, fallback: String = "Something went wrong"): String {
        if (throwable == null) return fallback

        return if (throwable.isConnectivityIssue()) {
            "Unable to connect to the server"
        } else {
            throwable.message?.takeIf { it.isNotBlank() } ?: fallback
        }
    }

    /**
     * Parses error body from Retrofit response and returns a friendly message.
     * Specific handling for "Delivery access required" -> "please contact Administrator"
     */
    fun parseError(response: Response<*>?, fallback: String = "Something went wrong"): String {
        if (response == null) return fallback
        
        val errorBody = try {
            response.errorBody()?.string()
        } catch (e: Exception) {
            null
        }

        if (errorBody.isNullOrBlank()) return fallback

        return try {
            val json = JSONObject(errorBody)
            val detail = json.optString("detail")
            val message = json.optString("message")
            val error = json.optString("error")

            val rawMessage = when {
                !detail.isNullOrBlank() -> detail
                !message.isNullOrBlank() -> message
                !error.isNullOrBlank() -> error
                else -> null
            }

            if (rawMessage?.contains("Delivery access required", ignoreCase = true) == true) {
                "please contact Administratir"
            } else {
                rawMessage ?: fallback
            }
        } catch (e: Exception) {
            fallback
        }
    }

    private fun Throwable.isConnectivityIssue(): Boolean {
        if (this is UnknownHostException || this is ConnectException || this is SocketTimeoutException) {
            return true
        }

        if (this is IOException) {
            val text = message.orEmpty()
            if (
                text.contains("Unable to resolve host", ignoreCase = true) ||
                text.contains("Failed to connect", ignoreCase = true) ||
                text.contains("timeout", ignoreCase = true)
            ) {
                return true
            }
        }

        return (cause as? Throwable)?.isConnectivityIssue() == true
    }
}
