package com.svd.svdagencies.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

object OnDemandAgentLocation {
    const val REQUEST_ID = "location_request_id"
    const val LAST_UPLOAD = "on_demand_location_last_upload"
    const val LAST_ERROR = "on_demand_location_last_error"
    private const val ENABLED = "on_demand_location_enabled"
    private const val OWNER = "on_demand_location_owner"
    private const val CHANNEL = "location_requests"
    private const val NOTICE = 4902
    fun enabled(context: Context): Boolean {
        val session = SessionManager(context)
        val prefs = context.getSharedPreferences("svd_session", Context.MODE_PRIVATE)
        return session.getRole() == UserRole.DELIVERY && !session.getToken().isNullOrBlank() &&
            prefs.getBoolean(ENABLED, false) && prefs.getInt(OWNER, -1) == session.getUserId()
    }
    fun setEnabled(context: Context, value: Boolean) {
        context.getSharedPreferences("svd_session", Context.MODE_PRIVATE).edit()
            .putBoolean(ENABLED, value).putInt(OWNER, SessionManager(context).getUserId()).apply()
        if (!value) context.stopService(Intent(context, AgentLocationRequestService::class.java))
    }
    fun start(context: Context, requestId: String): Boolean = try {
        ContextCompat.startForegroundService(context, Intent(context, AgentLocationRequestService::class.java).putExtra(REQUEST_ID, requestId))
        true
    } catch (_: RuntimeException) { false }

    fun notifyRequest(context: Context, requestId: String) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(CHANNEL, "Admin location requests", NotificationManager.IMPORTANCE_HIGH))
        val open = Intent(context, AgentLocationActivity::class.java).putExtra(REQUEST_ID, requestId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val action = PendingIntent.getActivity(context, NOTICE, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        manager.notify(NOTICE, NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation).setContentTitle("Admin requested your location")
            .setContentText("Tap to share one current location with admin. Request expires in two minutes.")
            .setContentIntent(action).setAutoCancel(true).setTimeoutAfter(120000L).build())
    }

    fun receive(context: Context, data: Map<String, String>) {
        if (SessionManager(context).getRole() != UserRole.DELIVERY) return
        if (data["agent_id"] != SessionManager(context).getUserId().toString()) return
        val id = data["request_id"] ?: return
        if (runCatching { java.util.UUID.fromString(id) }.isFailure) return
        val expiresAt = data["expires_at_ms"]?.toLongOrNull() ?: return
        if (expiresAt <= System.currentTimeMillis()) return
        // Always visible to the agent, even if automatic capture succeeds.
        notifyRequest(context, id)
        val backgroundAllowed = Build.VERSION.SDK_INT < 29 || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (enabled(context) && backgroundAllowed) start(context, id)
    }
}
