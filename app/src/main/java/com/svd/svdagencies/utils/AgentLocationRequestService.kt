package com.svd.svdagencies.utils

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.*
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/** One request, at most 45 seconds of GPS, then one upload and complete cleanup. */
class AgentLocationRequestService : Service(), LocationListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("svd_session", MODE_PRIVATE) }
    private var manager: LocationManager? = null
    private var pending: Call<*>? = null
    private var requestId: String? = null
    private var token: String? = null
    private var owner = -1
    private var startedAt = 0L
    private var best: Location? = null
    private var uploading = false
    private var finished = false
    private val deadline = Runnable { if (!uploading) { best?.let { upload(it) } ?: fail("timeout") } }
    private val hardDeadline = Runnable { finishRequest() }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getStringExtra(OnDemandAgentLocation.REQUEST_ID)
        if (id.isNullOrBlank()) { stopSelf(); return START_NOT_STICKY }
        if (requestId != null) return START_NOT_STICKY
        requestId = id
        token = SessionManager(this).getToken()
        owner = SessionManager(this).getUserId()
        if (token.isNullOrBlank() || SessionManager(this).getRole() != UserRole.DELIVERY) { stopSelf(); return START_NOT_STICKY }
        val notifications = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) notifications.createNotificationChannel(NotificationChannel("location_capture", "Requested location capture", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 4903, Intent(this, AgentLocationActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notice = NotificationCompat.Builder(this, "location_capture").setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Getting location for admin").setContentText("GPS stops automatically after this request.")
            .setOngoing(true).setContentIntent(open).build()
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(4903, notice, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION) else startForeground(4903, notice)
        } catch (_: RuntimeException) { OnDemandAgentLocation.notifyRequest(this, id); stopSelf(); return START_NOT_STICKY }
        prefs.registerOnSharedPreferenceChangeListener(this)
        handler.postDelayed(hardDeadline, 90000L)
        val call = LiveLocationNetwork.api.locationRequest(id)
        pending = call
        call.enqueue(object : Callback<LocationRequestEnvelope> {
            override fun onResponse(call: Call<LocationRequestEnvelope>, response: Response<LocationRequestEnvelope>) {
                if (pending !== call || finished) return
                pending = null
                if (!sameSession() || !response.isSuccessful || response.body()?.request?.status != "pending") { finishRequest(); return }
                capture()
            }
            override fun onFailure(call: Call<LocationRequestEnvelope>, error: Throwable) {
                if (pending !== call || finished) return
                rememberError("Could not check admin request. Check internet and try again."); finishRequest()
            }
        })
        return START_NOT_STICKY
    }
    private fun sameSession() = SessionManager(this).getToken() == token && SessionManager(this).getUserId() == owner && SessionManager(this).getRole() == UserRole.DELIVERY
    private fun capture() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) { fail("permission"); return }
        manager = getSystemService(LOCATION_SERVICE) as LocationManager
        startedAt = SystemClock.elapsedRealtime()
        val providers = if (fine) listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER) else listOf(LocationManager.NETWORK_PROVIDER)
        var available = false
        try {
            for (provider in providers) {
                if (provider !in manager!!.allProviders || !manager!!.isProviderEnabled(provider)) continue
                available = true
                manager!!.requestLocationUpdates(provider, 1000L, 0f, this, Looper.getMainLooper())
            }
        } catch (_: SecurityException) { fail("permission"); return }
        if (!available) { fail("gps_off"); return }
        handler.postDelayed(deadline, 45000L)
    }
    override fun onLocationChanged(location: Location) {
        if (finished || uploading || startedAt == 0L || !sameSession()) return
        val fixAt = location.elapsedRealtimeNanos / 1000000L
        val age = SystemClock.elapsedRealtime() - fixAt
        // Never satisfy an admin request with a position from before capture.
        if (fixAt < startedAt || age !in 0L..15000L || !location.hasAccuracy()) return
        if (best == null || location.accuracy < best!!.accuracy) best = Location(location)
        if (location.accuracy <= 100f) upload(location)
    }
    private fun upload(location: Location) {
        val age = SystemClock.elapsedRealtime() - location.elapsedRealtimeNanos / 1000000L
        if (age !in 0L..60000L) { fail("timeout"); return }
        val capturedAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US)
            .format(java.util.Date(System.currentTimeMillis() - age))
        respond(mapOf("latitude" to location.latitude, "longitude" to location.longitude,
            "accuracy" to location.accuracy, "captured_at" to capturedAt))
    }
    private fun fail(reason: String) {
        rememberError(when(reason) { "gps_off" -> "Turn on phone location and try again."; "permission" -> "Location permission is required."; else -> "No fresh GPS fix. Try again outdoors." })
        respond(mapOf("status" to "failed", "reason" to reason))
    }
    private fun respond(body: Map<String, Any>) {
        if (finished || uploading) return
        if (!sameSession()) { finishRequest(); return }
        uploading = true
        handler.removeCallbacks(deadline)
        manager?.removeUpdates(this); manager = null
        val call = LiveLocationNetwork.api.answerLocationRequest(requestId!!, body)
        pending = call
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (pending !== call || finished) return
                if (sameSession()) {
                    if (response.isSuccessful && body["status"] != "failed") prefs.edit().putLong(OnDemandAgentLocation.LAST_UPLOAD, System.currentTimeMillis()).remove(OnDemandAgentLocation.LAST_ERROR).apply()
                    else if (!response.isSuccessful) rememberError("Request expired or upload failed. Ask admin to request again.")
                }
                finishRequest()
            }
            override fun onFailure(call: Call<Void>, error: Throwable) {
                if (pending !== call || finished) return
                if (sameSession()) rememberError("Location upload failed. Check internet and request again.")
                finishRequest()
            }
        })
    }
    private fun rememberError(message: String) { if (sameSession()) prefs.edit().putString(OnDemandAgentLocation.LAST_ERROR, message).apply() }
    override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) { if (!sameSession()) finishRequest() }
    private fun finishRequest() {
        if (finished) return
        finished = true
        handler.removeCallbacksAndMessages(null)
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        manager?.removeUpdates(this); manager = null
        pending?.cancel(); pending = null
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }
    override fun onDestroy() { finishRequest(); super.onDestroy() }
    override fun onProviderEnabled(provider: String) = Unit
    override fun onProviderDisabled(provider: String) = Unit
    @Deprecated("Legacy Android callback") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
}
