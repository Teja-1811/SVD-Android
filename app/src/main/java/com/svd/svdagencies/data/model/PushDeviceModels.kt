package com.svd.svdagencies.data.model

import com.google.gson.annotations.SerializedName

data class PushDeviceRegisterRequest(
    val token: String,
    @SerializedName("device_type")
    val deviceType: String = "android",
    @SerializedName("device_name")
    val deviceName: String = "",
    @SerializedName("app_version")
    val appVersion: String = "",
    @SerializedName("user_agent")
    val userAgent: String = ""
)

data class PushDeviceResponse(
    val success: Boolean = false,
    @SerializedName("device_id")
    val deviceId: Int? = null,
    val message: String? = null
)
