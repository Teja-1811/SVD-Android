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
    fun enabled(context: Context) = context.getSharedPreferences("svd_session", Context.MODE_PRIVATE).getBoolean(ENABLED, false)
    fun start(context: Context): Boolean = try {
        ContextCompat.startForegroundService(context, Intent(context, AgentLocationService::class.java))
        true
    } catch (error: RuntimeException) {
        Log.w("ForegroundAgentLocation", "Could not start location-sharing service", error)
        false
    }
    fun setEnabled(activity: Activity, enabled: Boolean) {
        val prefs = activity.getSharedPreferences("svd_session", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(ENABLED, enabled).apply()
        if (enabled) {
            if (!start(activity)) {
                // Keep the agent's explicit opt-in.  Android can reject a
                // foreground-service start temporarily; opening the sharing
                // screen again will retry instead of silently switching off.
                Toast.makeText(activity, "Could not start location updates yet. Check permissions and try again.", Toast.LENGTH_LONG).show()
            }
        } else activity.startService(Intent(activity, AgentLocationService::class.java).setAction(AgentLocationService.STOP))
    }
}
