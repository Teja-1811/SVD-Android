package com.svd.svdagencies.utils

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.svd.svdagencies.R
import com.svd.svdagencies.base.BaseActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AgentLocationActivity : BaseActivity() {
    private lateinit var list: LinearLayout
    private lateinit var status: TextView
    private var liveMap: EmbeddedAgentMap? = null
    private val handler = Handler(Looper.getMainLooper())
    private var pending: Call<AgentPositions>? = null
    private var requestedId: String? = null
    private var requestCall: Call<LocationRequestEnvelope>? = null
    private val preferenceListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (active && !admin && key in setOf(OnDemandAgentLocation.LAST_UPLOAD, OnDemandAgentLocation.LAST_ERROR)) showSharing()
    }
    private var active = false
    private val refresh = Runnable { loadAgents() }
    // The activity is shared by the delivery and admin portals.  The portal
    // that opened it is authoritative: a stale stored role must not turn an
    // admin's Agent Locations screen into the delivery sharing screen.
    private val admin get() = intent.getBooleanExtra(EXTRA_ADMIN_LOCATIONS, false) ||
        SessionManager(this).getRole() == UserRole.ADMIN
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun color(id: Int) = ContextCompat.getColor(this, id)
    private fun primary() = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, color(R.color.colorPrimary))

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val shell = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(color(R.color.window_background)) }
        val toolbar = MaterialToolbar(this).apply {
            title = if (admin) "Agent Locations" else "Location sharing"
            setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            navigationContentDescription = "Back"
            setNavigationOnClickListener { finish() }
        }
        shell.addView(toolbar, LinearLayout.LayoutParams(-1, dp(56)))
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(24)) }
        val scroll = NestedScrollView(this).apply { isFillViewport = true; clipToPadding = false; addView(list, ViewGroup.LayoutParams(-1, -2)) }
        if (admin) {
            liveMap = EmbeddedAgentMap(this)
            shell.addView(liveMap, LinearLayout.LayoutParams(-1, 0, 1f))
        } else shell.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(shell)
        if (admin) { status = label("Loading agent locations…", 14f); list.addView(status) }
        else {
            com.svd.svdagencies.notifications.PushRegistrationManager.registerCurrentDevice(this)
            showSharing()
        }
    }

    private fun label(value: String, size: Float = 14f, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color(if (bold) R.color.textColorPrimary else R.color.textColorSecondary))
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setLineSpacing(dp(3).toFloat(), 1f)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }
    }
    private fun card(): LinearLayout {
        val frame = MaterialCardView(this).apply {
            radius = dp(16).toFloat(); cardElevation = 0f; strokeWidth = dp(1)
            setStrokeColor(color(R.color.card_stroke_color)); setCardBackgroundColor(color(R.color.surface_background))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) }
        }
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(12)) }
        frame.addView(body); list.addView(frame); return body
    }
    private fun button(value: String, click: () -> Unit) = MaterialButton(this).apply {
        text = value; isAllCaps = false; textSize = 15f; cornerRadius = dp(12); minHeight = dp(48)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8); bottomMargin = dp(8) }
        setOnClickListener { click() }
    }
    private fun showSharing() {
        list.removeAllViews()
        val enabled = OnDemandAgentLocation.enabled(this)
        val body = card()
        body.addView(label("LOCATION ON REQUEST", 12f, true))
        body.addView(label("Share only when needed", 23f, true))
        body.addView(label("Admin can tap Get location to request one current position. GPS stays off between requests. Only admin can view requested locations."))
        body.addView(label(if (enabled) "Automatic replies are on" else "Tap a request notification to share", 15f, true))
        body.addView(button(if (enabled) "Turn off automatic replies" else "Allow automatic replies") {
            if (enabled) OnDemandAgentLocation.setEnabled(this, false) else enableSharing()
            showSharing()
        })
        requestedId?.let { id ->
            body.addView(label("Admin is waiting for one location.", 14f, true))
            body.addView(button("Share requested location now") { shareOnce(id) })
        }
        body.addView(button("Check for admin request") { loadPendingRequest(false) })
        val preferences = getSharedPreferences("svd_session", MODE_PRIVATE)
        val last = preferences.getLong(OnDemandAgentLocation.LAST_UPLOAD, 0L)
        val error = preferences.getString(OnDemandAgentLocation.LAST_ERROR, null)
        if (last > 0) body.addView(label("Last shared: " + java.text.DateFormat.getDateTimeInstance().format(java.util.Date(last))))
        if (error != null) body.addView(label(error).apply { setTextColor(color(R.color.icon_red)) })
        val conditions = card()
        conditions.addView(label("Phone settings", 17f, true))
        conditions.addView(label("Internet and phone location must be on. Automatic replies while SVD is closed need ‘Allow all the time’ location access. Android may still require you to tap the notification."))
        conditions.addView(button("Open app permissions") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        })
        conditions.addView(label("On Vivo, allow SVD under High background battery usage. Force-stopping SVD blocks requests until you reopen it."))
        conditions.addView(button("Open background battery settings") {
            runCatching { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                .onFailure { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }
        })
        val info = card()
        info.addView(label("Your location, one request at a time", 17f, true))
        info.addView(label("Each request expires after two minutes. Location capture runs for at most 45 seconds, then stops. We keep only the latest requested position, not your journey history. Existing public tracking links do not receive these private replies."))
    }
    private fun enableSharing() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 271); return
        }
        if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 272); return
        }
        OnDemandAgentLocation.setEnabled(this, true); showSharing()
        if (android.os.Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            android.widget.Toast.makeText(this, "For automatic replies while closed, set Location to Allow all the time in app permissions.", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    private fun shareOnce(id: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestedId = id
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 273); return
        }
        val started = OnDemandAgentLocation.start(this, id)
        android.widget.Toast.makeText(this, if (started) "Getting one location for admin…" else "Could not start. Check phone permissions and try again.", android.widget.Toast.LENGTH_LONG).show()
        if (started) { requestedId = null; showSharing() }
    }
    private fun loadPendingRequest(fromNotification: Boolean) {
        requestCall?.cancel()
        val notifiedId = if (fromNotification) intent.getStringExtra(OnDemandAgentLocation.REQUEST_ID) else null
        val call = if (notifiedId != null) LiveLocationNetwork.api.locationRequest(notifiedId) else LiveLocationNetwork.api.pendingLocationRequest()
        requestCall = call
        call.enqueue(object : Callback<LocationRequestEnvelope> {
            override fun onResponse(call: Call<LocationRequestEnvelope>, response: Response<LocationRequestEnvelope>) {
                if (requestCall !== call || !active) return
                requestCall = null
                val inquiry = response.body()?.request
                requestedId = inquiry?.takeIf { it.status == "pending" }?.request_id
                showSharing()
                if (fromNotification) {
                    intent.removeExtra(OnDemandAgentLocation.REQUEST_ID)
                    if (requestedId == null) android.widget.Toast.makeText(this@AgentLocationActivity, "This request expired or is already answered.", android.widget.Toast.LENGTH_LONG).show()
                    requestedId?.let { shareOnce(it) }
                }
            }
            override fun onFailure(call: Call<LocationRequestEnvelope>, error: Throwable) {
                if (requestCall !== call || !active) return
                requestCall = null
                android.widget.Toast.makeText(this@AgentLocationActivity, "Could not check requests. Check internet and try again.", android.widget.Toast.LENGTH_LONG).show()
            }
        })
    }
    override fun onRequestPermissionsResult(code: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, permissions, results)
        if (results.any { it == PackageManager.PERMISSION_GRANTED }) {
            if (code in setOf(271, 272)) enableSharing()
            if (code == 273) requestedId?.let { shareOnce(it) }
        }
    }
    override fun onResume() {
        super.onResume(); active = true; liveMap?.onResume()
        if (admin) loadAgents() else {
            // Upgrading must not restart the old continuous GPS service.
            stopService(Intent(this, AgentLocationService::class.java))
            getSharedPreferences("svd_session", MODE_PRIVATE).registerOnSharedPreferenceChangeListener(preferenceListener)
            showSharing()
            loadPendingRequest(intent.hasExtra(OnDemandAgentLocation.REQUEST_ID))
        }
    }
    override fun onPause() {
        getSharedPreferences("svd_session", MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(preferenceListener)
        requestCall?.cancel(); requestCall = null
        active = false; liveMap?.onPause(); handler.removeCallbacks(refresh); pending?.cancel(); pending = null; super.onPause() }
    override fun onDestroy() { liveMap?.destroy(); super.onDestroy() }
    private fun loadAgents() {
        if (!active || !admin || pending != null) {
            Log.d(TAG, "Skipping agent request: active=$active admin=$admin pending=${pending != null}")
            return
        }
        Log.d(TAG, "Requesting admin live locations")
        pending = LiveLocationNetwork.api.agents()
        pending?.enqueue(object : Callback<AgentPositions> {
            override fun onResponse(call: Call<AgentPositions>, response: Response<AgentPositions>) {
                if (pending !== call) return
                pending = null
                if (!active) return
                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "Loaded ${response.body()!!.agents.size} agent locations")
                    liveMap?.update(response.body()!!)
                } else {
                    Log.w(TAG, "Live locations request failed: HTTP ${response.code()}")
                    liveMap?.offline()
                }
                handler.postDelayed(refresh, 15000)
            }
            override fun onFailure(call: Call<AgentPositions>, error: Throwable) {
                if (pending !== call) return
                pending = null
                if (!active) return
                Log.w(TAG, "Live locations request failed", error)
                liveMap?.offline(); handler.postDelayed(refresh, 15000)
            }
        })
    }

    companion object {
        const val EXTRA_ADMIN_LOCATIONS = "admin_agent_locations"
        private const val TAG = "AgentLocationActivity"
    }
}
