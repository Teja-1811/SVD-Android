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
        else showSharing()
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
        val sharing = ForegroundAgentLocation.enabled(this)
        val manager = getSystemService(LOCATION_SERVICE) as LocationManager
        val gps = runCatching { manager.isProviderEnabled(LocationManager.GPS_PROVIDER) || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
        val body = card()
        body.addView(label("DELIVERY LOCATION", 12f, true).apply { setTextColor(primary()); letterSpacing = .08f })
        body.addView(label(if (sharing) "Sharing is on" else "You control your location", 23f, true))
        body.addView(label(if (sharing) "Admin and people with an active tracking link can view your shared location." else "Share your delivery location. Admin can create a 24-hour tracking link for customers or others."))
        body.addView(label(if (sharing) "●  SHARING ENABLED" else "●  SHARING OFF", 12f, true).apply {
            setTextColor(color(if (sharing) R.color.icon_green else R.color.textColorSecondary))
            setPadding(0, dp(4), 0, dp(4)); accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        })
        if (sharing) {
            val prefs = getSharedPreferences("svd_session", MODE_PRIVATE)
            val lastUpload = prefs.getLong(ForegroundAgentLocation.LAST_UPLOAD_AT, 0L)
            val error = prefs.getString(ForegroundAgentLocation.LAST_ERROR, null)
            val serviceText = when {
                error != null -> "Location update needs attention: $error"
                lastUpload > 0L -> "Last uploaded: ${java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(lastUpload))}"
                else -> "Waiting for the first location update…"
            }
            body.addView(label(serviceText, 13f).apply { setTextColor(color(if (error == null) R.color.textColorSecondary else R.color.icon_red)) })
        }
        body.addView(button(if (sharing) "Stop sharing" else "Start sharing") {
            if (ForegroundAgentLocation.enabled(this)) { ForegroundAgentLocation.setEnabled(this, false); showSharing() }
            else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) enableSharing()
            else requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 271)
        }.apply { icon = ContextCompat.getDrawable(this@AgentLocationActivity, if (sharing) android.R.drawable.ic_media_pause else android.R.drawable.ic_menu_mylocation) })

        val conditions = card()
        conditions.addView(label("Location settings", 17f, true))
        conditions.addView(label(if (gps) "Phone location is on" else "Phone location is off", 14f, true).apply {
            setTextColor(color(if (gps) R.color.icon_green else R.color.icon_red))
        })
        conditions.addView(label("GPS and an internet connection are needed for fresh updates. Accuracy may vary."))
        if (!gps) conditions.addView(button("Open location settings") { startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) })
        if (sharing && android.os.Build.VERSION.SDK_INT >= 29 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            conditions.addView(label("For sharing to resume after a phone restart, set Location access to ‘Allow all the time’."))
            conditions.addView(button("Open app location permission") {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            })
        }

        val info = card()
        info.addView(label("How sharing works", 17f, true))
        info.addView(label("Screen off or app closed", 14f, true))
        info.addView(label("Sharing continues until you stop it or sign out. The notification also has a Stop button."))
        info.addView(label("Your privacy", 14f, true))
        info.addView(label("Only your latest position is stored. Public links expire after 24 hours and can be revoked. Stop sharing to hide your position. Force-stopping the app or restarting your phone ends sharing."))
    }
    private fun enableSharing() {
        if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 272); return
        }
        ForegroundAgentLocation.setEnabled(this, true); showSharing()
    }
    override fun onRequestPermissionsResult(code: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, permissions, results)
        if (code in setOf(271, 272) && results.any { it == PackageManager.PERMISSION_GRANTED }) enableSharing()
    }
    override fun onResume() {
        super.onResume(); active = true; liveMap?.onResume()
        if (admin) loadAgents() else {
            // Resume only a previous explicit opt-in, from a visible screen.
            if (ForegroundAgentLocation.enabled(this)) ForegroundAgentLocation.setEnabled(this, true)
            showSharing()
        }
    }
    override fun onPause() { active = false; liveMap?.onPause(); handler.removeCallbacks(refresh); pending?.cancel(); pending = null; super.onPause() }
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
