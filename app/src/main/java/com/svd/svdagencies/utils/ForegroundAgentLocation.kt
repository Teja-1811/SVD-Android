package com.svd.svdagencies.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.ContextCompat

object ForegroundAgentLocation {
    const val ENABLED = "share_background_location"
    fun enabled(context: Context) = context.getSharedPreferences("svd_session", Context.MODE_PRIVATE).getBoolean(ENABLED, false)
    fun setEnabled(activity: Activity, enabled: Boolean) {
        val prefs = activity.getSharedPreferences("svd_session", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(ENABLED, enabled).apply()
        val intent = Intent(activity, AgentLocationService::class.java)
        if (enabled) {
            try { ContextCompat.startForegroundService(activity, intent) }
            catch (_: RuntimeException) {
                prefs.edit().putBoolean(ENABLED, false).apply()
                Toast.makeText(activity, "Could not start sharing. Check location permissions and try again.", Toast.LENGTH_LONG).show()
            }
        } else activity.startService(intent.setAction(AgentLocationService.STOP))
    }
}
