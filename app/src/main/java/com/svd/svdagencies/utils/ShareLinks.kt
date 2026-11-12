package com.svd.svdagencies.utils

import java.net.URI

object ShareLinks {
    /** Remove copy/formatting breaks before handing one complete URL to WhatsApp. */
    fun normalize(raw: String?, baseUrl: String): String? {
        val clean = raw.orEmpty().replace(Regex("[\\s\\p{Z}\\p{Cf}\\p{Cc}]+"), "")
        if (clean.isEmpty()) return null
        return runCatching {
            val uri = URI(baseUrl).resolve(clean)
            require(uri.scheme in listOf("https", "http") && !uri.host.isNullOrBlank() && uri.userInfo == null)
            uri.toASCIIString()
        }.getOrNull()
    }
}
