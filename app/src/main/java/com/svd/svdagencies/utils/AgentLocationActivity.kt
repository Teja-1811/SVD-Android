package com.svd.svdagencies.utils

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.core.content.ContextCompat
import com.svd.svdagencies.base.BaseActivity
import com.svd.svdagencies.data.api.auth.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AgentLocationActivity : BaseActivity() {
    private lateinit var list: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private var pending: Call<AgentPositions>? = null
    private var active = false
    private val refresh = Runnable { loadAgents() }
    private val admin get() = SessionManager(this).getRole() == UserRole.ADMIN
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 24, 24, 80) }
        setContentView(ScrollView(this).apply { addView(list) })
        if (!admin) showSharing()
    }
    private fun text(value: String) { list.addView(TextView(this).apply { text = value; textSize = 17f; setPadding(0, 12, 0, 12) }) }
    private fun showSharing() {
        list.removeAllViews(); text("Delivery location sharing")
        text("Share your location with admin only while this app is on screen. Sharing stops when you leave the app or turn the screen off. Only your latest position is stored.")
        text(if (ForegroundAgentLocation.enabled(this)) "Location sharing is ON" else "Location sharing is OFF")
        list.addView(Button(this).apply {
            text = if (ForegroundAgentLocation.enabled(this@AgentLocationActivity)) "Stop sharing" else "Start sharing"
            setOnClickListener {
                if (ForegroundAgentLocation.enabled(this@AgentLocationActivity)) { ForegroundAgentLocation.setEnabled(this@AgentLocationActivity, false); showSharing() }
                else if (ContextCompat.checkSelfPermission(this@AgentLocationActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this@AgentLocationActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) enableSharing()
                else requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 271)
            }
        })
        text("Turn on the phone’s Location setting to send updates. GPS and network availability affect accuracy.")
    }
    private fun enableSharing() { ForegroundAgentLocation.setEnabled(this, true); showSharing() }
    override fun onRequestPermissionsResult(code: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, permissions, results)
        if (code == 271 && results.any { it == PackageManager.PERMISSION_GRANTED }) enableSharing()
    }
    override fun onResume() { super.onResume(); active = true; if (admin) loadAgents() else showSharing() }
    override fun onPause() { active = false; handler.removeCallbacks(refresh); pending?.cancel(); super.onPause() }
    private fun loadAgents() {
        if (!active || !admin) return
        pending = LiveLocationNetwork.api.agents()
        pending?.enqueue(object : Callback<AgentPositions> {
            override fun onResponse(call: Call<AgentPositions>, response: Response<AgentPositions>) {
                if (!active) return
                list.removeAllViews(); text("Agent Locations")
                if (response.isSuccessful && response.body() != null) {
                    val agents = response.body()!!.agents
                    if (agents.isEmpty()) text("No delivery agents found.")
                    for (agent in agents) {
                        text("${agent.name} · ${agent.route}\n${agent.status}\n${agent.updated_at ?: "No location received"}" + (agent.accuracy?.let { "\nAccuracy ±${it.toInt()} m" } ?: ""))
                        agent.maps_url?.let { url -> list.addView(Button(this@AgentLocationActivity).apply {
                            text = "View on map"; setOnClickListener { runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
                        }) }
                    }
                } else text("Unable to load locations. Admin access is required.")
                handler.postDelayed(refresh, 15000)
            }
            override fun onFailure(call: Call<AgentPositions>, error: Throwable) {
                if (!active) return
                list.removeAllViews(); text("Could not refresh locations. Check your connection."); handler.postDelayed(refresh, 15000)
            }
        })
    }
}
