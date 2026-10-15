package com.svd.svdagencies.data.api.auth

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class RegisterRequest(
    val name: String,
    val phone: String,
    val password: String,
    @SerializedName("account_type") val accountType: String,
    @SerializedName("terms_accepted") val termsAccepted: Boolean,
    @SerializedName("shop_name") val shopName: String = "",
    val area: String = ""
)

@Keep
data class RegistrationResponse(
    val status: String,
    val message: String? = null
)
