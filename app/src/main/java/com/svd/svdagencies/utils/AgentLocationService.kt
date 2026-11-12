package com.svd.svdagencies.utils

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.*
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.svd.svdagencies.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/** Started by the agent from a visible screen; continues with a Stop notification. */
class AgentLocationService : Service(), LocationListener, SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        const val STOP = "com.svd.svdagencies.STOP_LOCATION"
        private const val CHANNEL = "delivery_location"
        private const val NOTICE = 4901
        private const val TAG = "AgentLocationService"
    }
    private var manager: LocationManager? = null
    private var pending: Call<Void>? = null
    private val handler = Handler(Looper.getMainLooper())
    private var latestLocation: Location? = null
    private var lastFixAt = 0L
    private var lastRegistrationAt = 0L
    private val recovery = object : Runnable {
        override fun run() {
            if (!started) return
            if (!ForegroundAgentLocation.enabled(this@AgentLocationService)) {
                finishSharing(clearPreference = false)
                return
            }
            val now = SystemClock.elapsedRealtime()
            // Re-register only after a gap, never every tick: resetting GPS
            // repeatedly can prevent it from completing a fix indoors.
            if (now - maxOf(lastFixAt, lastRegistrationAt) >= 60000L) registerProviders()
            latestLocation?.let { onLocationChanged(it) }
            if (started) handler.postDelayed(this, 15000L)
        }
    }
    private var lastSent = 0L
    private var token: String? = null
    private var started = false
    private val prefs by lazy { getSharedPreferences("svd_session", MODE_PRIVATE) }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Only the agent's explicit Stop action clears their sharing choice.
        if (intent?.action == STOP) { finishSharing(clearPreference = true); return START_NOT_STICKY }
        if (started) return START_STICKY
        token = SessionManager(this).getToken()
        if (!ForegroundAgentLocation.enabled(this) || SessionManager(this).getRole() != UserRole.DELIVERY || token.isNullOrBlank()) {
            stopForTemporaryCondition(); return START_NOT_STICKY
        }
        val precise = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!precise && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            stopForTemporaryCondition(); return START_NOT_STICKY
        }
        val notifications = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) notifications.createNotificationChannel(NotificationChannel(CHANNEL, "Delivery location sharing", NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(this, 1, Intent(this, AgentLocationService::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val open = PendingIntent.getActivity(this, 2, Intent(this, AgentLocationActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation).setContentTitle("Sharing delivery location")
            .setContentText("Admin and active tracking-link recipients can view. Tap Stop to end sharing.")
            .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop sharing", stop).build()
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(NOTICE, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            else startForeground(NOTICE, notification)
            started = true
            prefs.registerOnSharedPreferenceChangeListener(this)
            manager = getSystemService(LOCATION_SERVICE) as LocationManager
            registerProviders()
            handler.postDelayed(recovery, 15000L)
        } catch (_: SecurityException) { stopForTemporaryCondition() }
        return if (started) START_STICKY else START_NOT_STICKY
    }
    private fun registerProviders() {
        val locationManager = manager ?: return
        val precise = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val providers = if (precise) listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER) else listOf(LocationManager.NETWORK_PROVIDER)
        lastRegistrationAt = SystemClock.elapsedRealtime()
        for (provider in providers) {
            try {
                if (provider in locationManager.allProviders && locationManager.isProviderEnabled(provider)) {
                    locationManager.requestLocationUpdates(provider, 15000L, 0f, this, Looper.getMainLooper())
                    locationManager.getLastKnownLocation(provider)?.let { onLocationChanged(it) }
                }
            } catch (error: IllegalArgumentException) {
                Log.w(TAG, "Location provider unavailable: $provider", error)
            } catch (_: SecurityException) {
                stopForTemporaryCondition()
                return
            }
        }
    }
    override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) {
        if (key == null || key in setOf("token", "role", ForegroundAgentLocation.ENABLED)) {
            if (!ForegroundAgentLocation.enabled(this)) finishSharing(clearPreference = false)
            else if (SessionManager(this).getRole() != UserRole.DELIVERY || SessionManager(this).getToken() != token) stopForTemporaryCondition()
        }
    }
    override fun onLocationChanged(location: Location) {
        if (!ForegroundAgentLocation.enabled(this)) { finishSharing(clearPreference = false); return }
        if (SessionManager(this).getToken() != token) { stopForTemporaryCondition(); return }
        val now = SystemClock.elapsedRealtime()
        val fixAt = location.elapsedRealtimeNanos / 1000000
        if (now - fixAt !in 0L..60000L) return
        if (fixAt >= lastFixAt) {
            lastFixAt = fixAt
            latestLocation = Location(location)
        } else return
        if ((lastSent != 0L && now - lastSent < 15000) || pending != null) return
        lastSent = now
        // A cached provider fix can have an old wall-clock timestamp even when
        // its elapsed-realtime age is acceptable. The server validates receipt
        // time, so send the current timestamp after the freshness check above.
        val stamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US).format(java.util.Date())
        val call = LiveLocationNetwork.api.send(mapOf("latitude" to location.latitude, "longitude" to location.longitude, "accuracy" to location.accuracy, "captured_at" to stamp))
        pending = call
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (pending !== call || !started) return
                pending = null
                if (response.isSuccessful) {
                    prefs.edit().putLong(ForegroundAgentLocation.LAST_UPLOAD_AT, System.currentTimeMillis())
                        .remove(ForegroundAgentLocation.LAST_ERROR).apply()
                    Log.d(TAG, "Live location uploaded")
                } else {
                    prefs.edit().putString(ForegroundAgentLocation.LAST_ERROR, "Server response ${response.code()}").apply()
                    Log.w(TAG, "Live location upload failed: HTTP ${response.code()}")
                }
                // A temporary authentication/session issue must not silently
                // revoke the agent's explicit opt-in.  The next foreground
                // app visit restarts this sticky service with the fresh token.
                if (response.code() in setOf(401, 403)) stopForTemporaryCondition()
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                if (pending !== call || !started) return
                pending = null
                prefs.edit().putString(ForegroundAgentLocation.LAST_ERROR, t.message ?: "Network error").apply()
                Log.w(TAG, "Live location upload failed", t)
            }
        })
    }
    private fun stopForTemporaryCondition() = finishSharing(clearPreference = false)

    private fun finishSharing(clearPreference: Boolean) {
        handler.removeCallbacks(recovery)
        latestLocation = null
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        if (clearPreference) prefs.edit().putBoolean(ForegroundAgentLocation.ENABLED, false).apply()
        manager?.removeUpdates(this); manager = null
        pending?.cancel(); pending = null
        // Notify the server only when the agent explicitly stopped sharing;
        // stopping for a temporary condition must keep their last position.
        if (clearPreference && started && SessionManager(this).getToken() == token) LiveLocationNetwork.api.stop().enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) = Unit
            override fun onFailure(call: Call<Void>, t: Throwable) = Unit
        })
        started = false
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }
    override fun onDestroy() {
        // System cleanup is not a user request to stop sharing. Preserve the
        // opt-in for a service restart; Stop and sign-out use finishSharing.
        handler.removeCallbacks(recovery)
        latestLocation = null
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        manager?.removeUpdates(this); manager = null
        pending?.cancel(); pending = null
        super.onDestroy()
    }
    override fun onProviderEnabled(provider: String) {
        if (!started || !ForegroundAgentLocation.enabled(this)) return
        if (provider !in setOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)) return
        if (provider == LocationManager.GPS_PROVIDER && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        try {
            manager?.requestLocationUpdates(provider, 15000L, 0f, this, Looper.getMainLooper())
        } catch (_: SecurityException) { stopForTemporaryCondition() }
    }
    override fun onProviderDisabled(provider: String) = Unit
    @Deprecated("Legacy Android callback") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

}
