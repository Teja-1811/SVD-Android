package com.svd.svdagencies.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/** Restores a delivery agent's explicit location-sharing choice after restart. */
class AgentLocationRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action !in ACTIONS || !ForegroundAgentLocation.enabled(context)) return
        if (SessionManager(context).getRole() != UserRole.DELIVERY || SessionManager(context).getToken().isNullOrBlank()) return
        if (!ForegroundAgentLocation.start(context)) {
            Log.w(TAG, "Location sharing remains enabled but Android deferred its restart")
        }
    }

    companion object {
        private const val TAG = "AgentLocationRestart"
        private val ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_USER_UNLOCKED,
        )
    }
}
