package com.svd.svdagencies.data.model.user

import com.google.gson.annotations.SerializedName

data class UserSubscriptionPause(
    val id: Int? = null,
    @SerializedName("pause_date")
    val pauseDate: String?,
    @SerializedName("resume_date")
    val resumeDate: String?,
    val reason: String?,
    // Backend sometimes sends string status ("Paused"/"Resumed") instead of boolean flag.
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("is_resumed")
    val isResumed: Boolean = false
) {
    val isActivePause: Boolean
        get() = when {
            status.equals("Resumed", ignoreCase = true) -> false
            status.equals("Paused", ignoreCase = true) -> true
            else -> !isResumed
        }
}
