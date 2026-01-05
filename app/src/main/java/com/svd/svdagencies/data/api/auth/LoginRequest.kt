package com.svd.svdagencies.data.api.auth

data class LoginRequest(
    val phone: String,
    val password: String
)