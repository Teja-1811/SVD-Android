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
        status = text("Loading…", 14)
        val card = MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            setCardBackgroundColor(getColor(R.color.card_background))
            strokeWidth = dp(1)
            strokeColor = getColor(R.color.card_stroke_color)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }
        }
        val body = com.svd.svdagencies.utils.CompactLinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(16))
        }
        card.addView(body)
        fields.addView(card)
        heading(body, "Order hours · IST")
        val times = flow(body)
        openingButton = button(times, "Opening") { pickTime(true) }
        closingButton = button(times, "Closing") { pickTime(false) }
        heading(body, "Working days")
        val dayRow = flow(body)
        listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday").forEach { day ->
            MaterialCheckBox(this).apply {
                text = day.take(3); contentDescription = day; isChecked = true
                minHeight = dp(48); minWidth = dp(72)
                dayRow.addView(this); days.add(this)
            }
        }
        heading(body, "Ordering mode")
        overrideSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@AdminOrderTimingsActivity, com.svd.svdagencies.R.layout.delivery_dropdown_item,
                listOf("Use schedule", "Open now", "Closed now"))
        }
        body.addView(overrideSpinner, LinearLayout.LayoutParams(-1, dp(56)))
        val overrideActions = flow(body)
        expiryButton = button(overrideActions, "Ends\nUntil changed") { pickExpiry() }
        button(overrideActions, "Clear end time") { expiry = null; updateLabels() }
        val actions = flow(fields)
        saveButton = button(actions, "Save timings") { save() }
        reloadButton = button(actions, "Reload") { load() }
        updateLabels()
        load()
    }

    private fun text(value: String, size: Int): TextView = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(getColor(R.color.textColorSecondary))
        setPadding(0, dp(4), 0, dp(4)); fields.addView(this)
    }

    private fun heading(parent: LinearLayout, value: String) {
        parent.addView(TextView(this).apply {
            text = value; textSize = 14f; setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(getColor(R.color.textColorPrimary)); setPadding(0, dp(12), 0, dp(4))
        })
    }

    private fun flow(parent: LinearLayout) = OrderTimingButtonFlow(this).apply {
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }
        parent.addView(this)
    }

    private fun button(parent: ViewGroup, label: String, action: () -> Unit) = MaterialButton(this).apply {
        text = label; isAllCaps = false; textSize = 13f
        minHeight = dp(48); minWidth = dp(108)
        setPadding(dp(8), dp(8), dp(8), dp(8))
        cornerRadius = dp(16)
        layoutParams = ViewGroup.LayoutParams(-2, -2)
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
        openingButton.text = "Opening\n$opens"
        closingButton.text = "Closing\n$closes"
        val format = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).apply { timeZone = india }
        expiryButton.text = "Ends\n" + (expiry?.let { format.format(it.time) + " IST" } ?: "Until changed")
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
                message("Unable to reach the server. Tap Reload to retry.")
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
        reloadButton.text = if (value) "Please wait..." else "Reload"
    }

    override fun onDestroy() { pendingCall?.cancel(); super.onDestroy() }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun message(value: String) = Toast.makeText(this, value, Toast.LENGTH_LONG).show()
}


/** Equal-growth rows that wrap controls when the available width is exhausted. */
internal class OrderTimingButtonFlow(context: android.content.Context) : ViewGroup(context) {
    private val rows = mutableListOf<List<View>>()
    private val gap get() = (8 * resources.displayMetrics.density).toInt()

    override fun generateDefaultLayoutParams() = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        com.svd.svdagencies.utils.CompactUi.applyTree(this)
        val width = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED)
            resources.displayMetrics.widthPixels else MeasureSpec.getSize(widthMeasureSpec)
        val available = (width - paddingLeft - paddingRight).coerceAtLeast(0)
        rows.clear()
        var row = mutableListOf<View>()
        var used = 0
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.visibility == GONE) continue
            val refresh = com.svd.svdagencies.utils.CompactUi.isRefreshControl(child)
            val size = (48 * resources.displayMetrics.density + .5f).toInt()
            child.measure(MeasureSpec.makeMeasureSpec(if (refresh) minOf(size, available) else available, if (refresh) MeasureSpec.EXACTLY else MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(if (refresh) size else 0, if (refresh) MeasureSpec.EXACTLY else MeasureSpec.UNSPECIFIED))
            val needed = child.measuredWidth + if (row.isEmpty()) 0 else gap
            if (row.isNotEmpty() && used + needed > available) {
                rows.add(row); row = mutableListOf(); used = 0
            }
            if (row.isNotEmpty()) used += gap
            row.add(child); used += child.measuredWidth
        }
        if (row.isNotEmpty()) rows.add(row)
        var height = paddingTop + paddingBottom
        rows.forEachIndexed { index, children ->
            val remaining = (available - children.sumOf { it.measuredWidth } - gap * (children.size - 1)).coerceAtLeast(0)
            val grow = children.filterNot { com.svd.svdagencies.utils.CompactUi.isRefreshControl(it) }
            children.forEach { child ->
                val indexInGrow = grow.indexOf(child)
                val extra = if (indexInGrow < 0 || grow.isEmpty()) 0 else remaining / grow.size + if (indexInGrow < remaining % grow.size) 1 else 0
                child.measure(MeasureSpec.makeMeasureSpec(child.measuredWidth + extra, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(child.measuredHeight, MeasureSpec.EXACTLY))
            }
            height += children.maxOf { it.measuredHeight } + if (index == 0) 0 else gap
        }
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var y = paddingTop
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        rows.forEach { children ->
            var x = if (rtl) width - paddingRight else paddingLeft
            children.forEach { child ->
                if (rtl) { child.layout(x - child.measuredWidth, y, x, y + child.measuredHeight); x -= child.measuredWidth + gap }
                else { child.layout(x, y, x + child.measuredWidth, y + child.measuredHeight); x += child.measuredWidth + gap }
            }
            y += children.maxOf { it.measuredHeight } + gap
        }
    }
}
