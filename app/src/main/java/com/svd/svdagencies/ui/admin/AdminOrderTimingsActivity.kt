package com.svd.svdagencies.ui.admin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

interface AdminOrderTimingApi {
    @GET("api/admin/order-timings/")
    fun load(): Call<JsonObject>
    @POST("api/admin/order-timings/")
    fun save(@Body body: JsonObject): Call<JsonObject>
}

class AdminOrderTimingsActivity : AdminBaseActivity() {
    private val api by lazy { ApiClient.retrofit.create(AdminOrderTimingApi::class.java) }
    private val india = TimeZone.getTimeZone("Asia/Kolkata")
    private var opens = "09:00"
    private var closes = "16:30"
    private var expiry: Calendar? = null
    private var busy = false
    private var loaded = false
    private var pendingCall: Call<JsonObject>? = null
    private lateinit var fields: LinearLayout
    private lateinit var openingButton: MaterialButton
    private lateinit var closingButton: MaterialButton
    private lateinit var expiryButton: MaterialButton
    private lateinit var overrideSpinner: Spinner
    private lateinit var status: TextView
    private lateinit var saveButton: MaterialButton
    private lateinit var reloadButton: MaterialButton
    private val days = mutableListOf<MaterialCheckBox>()
    private val overrideValues = listOf("schedule", "open", "closed")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_order_timings)
        setupAdminLayout("Order Timings")
        fields = findViewById(R.id.orderTimingsFields)
        text("Regular order timings", 22)
        text("All times are in India time (Asia/Kolkata).", 14)
        text("User pre-orders are available 24 hours, with delivery at least 2 days ahead. Retailers can place regular orders only.", 14)
        status = text("Loading saved timings...", 16)
        val card = MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            setCardBackgroundColor(android.graphics.Color.WHITE)
            strokeWidth = dp(1)
            strokeColor = android.graphics.Color.LTGRAY
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
        }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(16))
        }
        card.addView(body)
        fields.addView(card)
        openingButton = button(body, "Opening time") { pickTime(true) }
        closingButton = button(body, "Closing time") { pickTime(false) }
        body.addView(TextView(this).apply { text = "Working days"; textSize = 18f; setPadding(0, dp(16), 0, dp(4)) })
        listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday").forEach { day ->
            MaterialCheckBox(this).apply { text = day; isChecked = true; body.addView(this); days.add(this) }
        }
        body.addView(TextView(this).apply { text = "Ordering override"; textSize = 18f; setPadding(0, dp(16), 0, dp(8)) })
        overrideSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@AdminOrderTimingsActivity, android.R.layout.simple_spinner_dropdown_item,
                listOf("Use schedule", "Open now", "Closed now"))
        }
        body.addView(overrideSpinner, LinearLayout.LayoutParams(-1, dp(56)))
        expiryButton = button(body, "Override ends: Until changed") { pickExpiry() }
        button(body, "Clear override end time") { expiry = null; updateLabels() }
        body.addView(TextView(this).apply {
            text = "An override without an end time stays active until you choose Use schedule. After its end time, ordering returns to the schedule. Closing earlier than opening means overnight hours; equal times mean all day on the selected days."
            textSize = 14f
            setPadding(0, dp(12), 0, dp(12))
        })
        saveButton = button(fields, "Save order timings") { save() }
        reloadButton = button(fields, "Reload saved timings") { load() }
        updateLabels()
        load()
    }

    private fun text(value: String, size: Int): TextView = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(android.graphics.Color.parseColor("#18343B"))
        setPadding(0, dp(6), 0, dp(6)); fields.addView(this)
    }

    private fun button(parent: LinearLayout, label: String, action: () -> Unit) = MaterialButton(this).apply {
        text = label; isAllCaps = false
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }
        setOnClickListener { action() }; parent.addView(this)
    }

    private fun pickTime(opening: Boolean) {
        val selected = (if (opening) opens else closes).split(":")
        TimePickerDialog(this, { _, hour, minute ->
            val chosen = String.format(Locale.US, "%02d:%02d", hour, minute)
            if (opening) opens = chosen else closes = chosen
            updateLabels()
        }, selected[0].toInt(), selected[1].toInt(), true).show()
    }

    private fun pickExpiry() {
        val selected = (expiry?.clone() as? Calendar) ?: Calendar.getInstance(india)
        DatePickerDialog(this, { _, year, month, day ->
            selected.set(year, month, day)
            TimePickerDialog(this, { _, hour, minute ->
                selected.set(Calendar.HOUR_OF_DAY, hour)
                selected.set(Calendar.MINUTE, minute)
                selected.set(Calendar.SECOND, 0)
                selected.set(Calendar.MILLISECOND, 0)
                if (selected.timeInMillis <= System.currentTimeMillis()) {
                    message("Choose a future override end time.")
                } else { expiry = selected; updateLabels() }
            }, selected.get(Calendar.HOUR_OF_DAY), selected.get(Calendar.MINUTE), true).show()
        }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun updateLabels() {
        openingButton.text = "Opening time: $opens"
        closingButton.text = "Closing time: $closes"
        val format = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).apply { timeZone = india }
        expiryButton.text = "Override ends: " + (expiry?.let { format.format(it.time) + " IST" } ?: "Until changed")
    }

    private fun load() {
        if (busy) return
        send(api.load())
    }

    private fun save() {
        if (busy || !loaded) return
        val values = JsonObject().apply {
            addProperty("opens_at", opens); addProperty("closes_at", closes)
            add("working_days", JsonArray().apply { days.forEachIndexed { index, day -> if (day.isChecked) add(index) } })
            addProperty("override", overrideValues[overrideSpinner.selectedItemPosition])
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply { timeZone = india }
            addProperty("override_until", expiry?.let { format.format(it.time) })
        }
        send(api.save(values))
    }

    private fun send(call: Call<JsonObject>) {
        setBusy(true)
        pendingCall = call
        call.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (isFinishing || isDestroyed) return
                val body = response.body()
                if (response.isSuccessful && body?.get("success")?.asBoolean == true) {
                    val values = body.getAsJsonObject("settings")
                    opens = values.get("opens_at").asString
                    closes = values.get("closes_at").asString
                    val selectedDays = values.getAsJsonArray("working_days").map { it.asInt }.toSet()
                    days.forEachIndexed { index, day -> day.isChecked = index in selectedDays }
                    overrideSpinner.setSelection(overrideValues.indexOf(values.get("override").asString).coerceAtLeast(0))
                    expiry = values.get("override_until")?.takeUnless { it.isJsonNull }?.asString?.let { raw ->
                        val normalized = raw.replace(Regex("\\.\\d+(?=[+-]|Z$)"), "")
                        val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(normalized)
                        parsed?.let { Calendar.getInstance(india).apply { time = it } }
                    }
                    status.text = body.get("current_status")?.asString.orEmpty()
                    loaded = true
                    updateLabels()
                    if (call.request().method == "POST") message("Order timings updated.")
                } else {
                    val error = runCatching { JsonParser.parseString(response.errorBody()?.string()).asJsonObject.get("message").asString }.getOrNull()
                    message(error ?: "Could not load or save order timings. Please retry.")
                }
                setBusy(false)
            }
            override fun onFailure(call: Call<JsonObject>, error: Throwable) {
                if (isFinishing || isDestroyed) return
                setBusy(false)
                message("Unable to reach the server. Tap Reload saved timings to retry.")
            }
        })
    }

    private fun setBusy(value: Boolean) {
        busy = value
        fun enable(view: View) {
            view.isEnabled = !value
            if (view is ViewGroup) for (index in 0 until view.childCount) enable(view.getChildAt(index))
        }
        enable(fields)
        saveButton.isEnabled = !value && loaded
        reloadButton.text = if (value) "Please wait..." else "Reload saved timings"
    }

    override fun onDestroy() { pendingCall?.cancel(); super.onDestroy() }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun message(value: String) = Toast.makeText(this, value, Toast.LENGTH_LONG).show()
}
