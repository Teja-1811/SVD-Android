package com.svd.svdagencies.data.api.auth

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class LoginRequest(
    @SerializedName("phone")
    val phone: String,
    
    @SerializedName("password")
    val password: String
)