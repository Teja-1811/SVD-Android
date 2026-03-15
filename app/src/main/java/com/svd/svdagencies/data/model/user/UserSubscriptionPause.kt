package com.svd.svdagencies.data.model.user

import com.google.gson.annotations.SerializedName

data class UserSubscriptionPause(
    val plan: String,
    @SerializedName("pause_date")
    val pauseDate: String?,
    @SerializedName("resume_date")
    val resumeDate: String?,
    val reason: String?,
    val status: String
)
