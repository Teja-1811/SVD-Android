package com.svd.svdagencies.utils

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.svd.svdagencies.data.api.auth.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object ForegroundAgentLocation : Application.ActivityLifecycleCallbacks, LocationListener {
    private var current: Activity? = null
    private var manager: LocationManager? = null
    private var pending: Call<Void>? = null
    private var lastSent = 0L
    private val badgeId = View.generateViewId()
    private val api get() = LiveLocationNetwork.api
    fun enabled(context: Context) = context.getSharedPreferences("svd_session", Context.MODE_PRIVATE).getBoolean("share_live_location", false)
    fun setEnabled(activity: Activity, enabled: Boolean) {
        activity.getSharedPreferences("svd_session", Context.MODE_PRIVATE).edit().putBoolean("share_live_location", enabled).apply()
        stopUpdates()
        if (enabled) start(activity) else {
            (activity.findViewById<View>(badgeId)?.parent as? android.view.ViewGroup)?.removeView(activity.findViewById(badgeId))
            api.stop().enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) = Unit
                override fun onFailure(call: Call<Void>, t: Throwable) = Unit
            })
        }
    }
    private fun start(activity: Activity) {
        val session = SessionManager(activity)
        if (!enabled(activity) || session.getRole() != UserRole.DELIVERY || session.getToken().isNullOrBlank()) return
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        current = activity
        val root = activity.findViewById<FrameLayout>(android.R.id.content)
        if (root.findViewById<View>(badgeId) == null) {
            val badge = TextView(activity).apply {
                id = badgeId; text = "Location sharing ON · Stop"; setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.rgb(15, 95, 65)); setPadding(16, 12, 16, 12)
                setOnClickListener { setEnabled(activity, false) }
            }
            root.addView(badge, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.END))
        }
        manager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            val precise = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val providers = if (precise) listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER) else listOf(LocationManager.NETWORK_PROVIDER)
            for (provider in providers) {
                if (manager?.isProviderEnabled(provider) == true) manager?.requestLocationUpdates(provider, 15000L, 0f, this, android.os.Looper.getMainLooper())
            }
        } catch (_: SecurityException) { stopUpdates() }
    }
    private fun stopUpdates() {
        manager?.removeUpdates(this); manager = null; current = null
        pending?.cancel(); pending = null; lastSent = 0
    }
    override fun onLocationChanged(location: Location) {
        val activity = current ?: return
        if (!enabled(activity) || SessionManager(activity).getRole() != UserRole.DELIVERY) { stopUpdates(); return }
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - location.elapsedRealtimeNanos / 1000000 > 60000 || now - lastSent < 15000 || pending != null) return
        lastSent = now
        val stamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US).format(java.util.Date(location.time))
        val call = api.send(mapOf("latitude" to location.latitude, "longitude" to location.longitude, "accuracy" to location.accuracy, "captured_at" to stamp))
        pending = call
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) { if (pending === call) pending = null }
            override fun onFailure(call: Call<Void>, t: Throwable) { if (pending === call) pending = null }
        })
    }
    override fun onActivityResumed(activity: Activity) {
        stopUpdates()
        activity.findViewById<View>(badgeId)?.let { (it.parent as? android.view.ViewGroup)?.removeView(it) }
        start(activity)
    }
    override fun onActivityPaused(activity: Activity) { if (current === activity) stopUpdates() }
    override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
    override fun onProviderEnabled(provider: String) = Unit
    override fun onProviderDisabled(provider: String) = Unit
    @Deprecated("Legacy Android callback") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
}
