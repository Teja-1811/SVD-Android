package com.svd.svdagencies.utils

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.*
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.svd.svdagencies.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/** Started by the agent from a visible screen; continues with a Stop notification. */
class AgentLocationService : Service(), LocationListener, SharedPreferences.OnSharedPreferenceChangeListener {
    companion object { const val STOP = "com.svd.svdagencies.STOP_LOCATION"; private const val CHANNEL = "delivery_location"; private const val NOTICE = 4901 }
    private var manager: LocationManager? = null
    private var pending: Call<Void>? = null
    private var lastSent = 0L
    private var token: String? = null
    private var started = false
    private val prefs by lazy { getSharedPreferences("svd_session", MODE_PRIVATE) }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) { finishSharing(); return START_NOT_STICKY }
        if (started) return START_NOT_STICKY
        token = SessionManager(this).getToken()
        if (!ForegroundAgentLocation.enabled(this) || SessionManager(this).getRole() != UserRole.DELIVERY || token.isNullOrBlank()) {
            finishSharing(); return START_NOT_STICKY
        }
        val precise = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!precise && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            finishSharing(); return START_NOT_STICKY
        }
        val notifications = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) notifications.createNotificationChannel(NotificationChannel(CHANNEL, "Delivery location sharing", NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(this, 1, Intent(this, AgentLocationService::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val open = PendingIntent.getActivity(this, 2, Intent(this, AgentLocationActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation).setContentTitle("Sharing delivery location with admin")
            .setContentText("Active with the app closed or screen off. Tap Stop to end sharing.")
            .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop sharing", stop).build()
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(NOTICE, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            else startForeground(NOTICE, notification)
            started = true
            prefs.registerOnSharedPreferenceChangeListener(this)
            manager = getSystemService(LOCATION_SERVICE) as LocationManager
            val providers = if (precise) listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER) else listOf(LocationManager.NETWORK_PROVIDER)
            for (provider in providers) {
                if (manager?.isProviderEnabled(provider) == true) manager?.requestLocationUpdates(provider, 15000L, 0f, this, Looper.getMainLooper())
            }
        } catch (_: SecurityException) { finishSharing() }
        return START_NOT_STICKY
    }
    override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) {
        if (key == null || key in setOf("token", "role", ForegroundAgentLocation.ENABLED)) {
            if (!ForegroundAgentLocation.enabled(this) || SessionManager(this).getRole() != UserRole.DELIVERY || SessionManager(this).getToken() != token) finishSharing()
        }
    }
    override fun onLocationChanged(location: Location) {
        if (!ForegroundAgentLocation.enabled(this) || SessionManager(this).getToken() != token) { finishSharing(); return }
        val now = SystemClock.elapsedRealtime()
        if (now - location.elapsedRealtimeNanos / 1000000 > 60000 || (lastSent != 0L && now - lastSent < 15000) || pending != null) return
        lastSent = now
        val stamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US).format(java.util.Date(location.time))
        val call = LiveLocationNetwork.api.send(mapOf("latitude" to location.latitude, "longitude" to location.longitude, "accuracy" to location.accuracy, "captured_at" to stamp))
        pending = call
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (pending === call) pending = null
                if (response.code() in setOf(401, 403)) finishSharing()
            }
            override fun onFailure(call: Call<Void>, t: Throwable) { if (pending === call) pending = null }
        })
    }
    private fun finishSharing() {
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        prefs.edit().putBoolean(ForegroundAgentLocation.ENABLED, false).apply()
        manager?.removeUpdates(this); manager = null
        pending?.cancel(); pending = null
        if (started && SessionManager(this).getToken() == token) LiveLocationNetwork.api.stop().enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) = Unit
            override fun onFailure(call: Call<Void>, t: Throwable) = Unit
        })
        started = false
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }
    override fun onDestroy() { finishSharing(); super.onDestroy() }
    override fun onProviderEnabled(provider: String) = Unit
    override fun onProviderDisabled(provider: String) = Unit
    @Deprecated("Legacy Android callback") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
}
