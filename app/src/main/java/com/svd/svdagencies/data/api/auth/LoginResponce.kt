package com.svd.svdagencies.data.api.auth

data class LoginResponse(
    val status: String,
    val message: String? = null,
    val token: String? = null,
    val role: String? = null,
    val user_id: Int? = null,
    val phone: String? = null,
    val name: String? = null
)
