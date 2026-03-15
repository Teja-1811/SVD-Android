package com.svd.svdagencies.utils

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
