package com.svd.svdagencies.data.api.auth

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class LoginResponse(
    val status: String,
    val message: String? = null,
    val token: String? = null,
    val role: String? = null,
    @SerializedName("user_id")
    val userId: Int? = null,
    val phone: String? = null,
    val name: String? = null,
    val capabilities: LoginCapabilities? = null
)

@Keep
data class LoginCapabilities(
    @SerializedName("push_registration")
    val pushRegistration: String? = null,
    @SerializedName("push_unregistration")
    val pushUnregistration: String? = null,
    @SerializedName("prepare_payment_order")
    val preparePaymentOrder: String? = null,
    @SerializedName("record_payment")
    val recordPayment: String? = null,
    @SerializedName("payment_result")
    val paymentResult: String? = null,
    @SerializedName("user_dashboard")
    val userDashboard: String? = null,
    @SerializedName("user_bills")
    val userBills: String? = null
)
