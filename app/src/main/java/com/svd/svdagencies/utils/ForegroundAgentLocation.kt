package com.svd.svdagencies.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat

object ForegroundAgentLocation {
    const val ENABLED = "share_background_location"
    const val LAST_UPLOAD_AT = "location_last_upload_at"
    const val LAST_ERROR = "location_last_error"
    fun enabled(context: Context): Boolean = false
    fun start(context: Context): Boolean {
        context.stopService(Intent(context, AgentLocationService::class.java))
        return true
    }
    fun setEnabled(activity: Activity, enabled: Boolean) {
        activity.getSharedPreferences("svd_session", Context.MODE_PRIVATE).edit().putBoolean(ENABLED, false).apply()
        activity.stopService(Intent(activity, AgentLocationService::class.java))
    }
}
