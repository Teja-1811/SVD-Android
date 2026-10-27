package com.svd.svdagencies.ui.admin

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.svd.svdagencies.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

internal class SubscriptionDashboardUi(private val activity: NativeAnalyticsActivity, content: LinearLayout, saved: Bundle?) {
    private val ink = androidx.core.content.ContextCompat.getColor(activity, com.svd.svdagencies.R.color.textColorPrimary)
    private val muted = androidx.core.content.ContextCompat.getColor(activity, com.svd.svdagencies.R.color.textColorSecondary)
    val plan: Spinner
    val status: Spinner
    val inputs = linkedMapOf<String, TextInputEditText>()
    val refresh: MaterialButton
    val manage: MaterialButton
    val message: TextView
    val results: LinearLayout
    private var filterJob: Job? = null
    private var lastPlan = 0
    private var lastStatus = 0

    init {
        val panel = activity.layoutInflater.inflate(R.layout.admin_subscriptions_content, content, false)
        content.addView(panel)
        plan = panel.findViewById(R.id.analyticsPlan)
        status = panel.findViewById(R.id.analyticsStatus)
        refresh = panel.findViewById(R.id.analyticsRefresh)
        manage = panel.findViewById(R.id.analyticsManage)
        message = panel.findViewById(R.id.analyticsMessage)
        results = panel.findViewById(R.id.analyticsResults)
        for ((key, id) in listOf("from_date" to R.id.analyticsFrom, "to_date" to R.id.analyticsTo, "customer" to R.id.analyticsCustomer)) {
            inputs[key] = panel.findViewById<TextInputEditText>(id).apply { setText(saved?.getString(key).orEmpty()) }
        }
        listOf(plan, status).forEach {
            it.adapter = ArrayAdapter(activity, R.layout.delivery_dropdown_item, listOf(if (it === plan) "All plans" else "All statuses"))
        }
    }

    fun bindAutomaticFilters(onChange: () -> Unit) {
        fun queue() { filterJob?.cancel(); filterJob = activity.lifecycleScope.launch { delay(400); onChange() } }
        inputs.values.forEach { it.doAfterTextChanged { queue() } }
        val selection = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val changed = if (parent === plan) position != lastPlan else position != lastStatus
                if (parent === plan) lastPlan = position else lastStatus = position
                if (changed) queue()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        plan.onItemSelectedListener = selection; status.onItemSelectedListener = selection
    }
    fun syncFilters() { filterJob?.cancel(); lastPlan = plan.selectedItemPosition; lastStatus = status.selectedItemPosition }

    fun render(data: JsonObject, onPage: (Int) -> Unit) {
        results.removeAllViews()
        val totals = data.getAsJsonObject("totals") ?: JsonObject()
        val primary = listOf("subscriptions", "active", "paused", "delivered", "net_paid", "outstanding")
        primary.chunked(2).forEach { keys ->
            val line = row()
            keys.forEachIndexed { i, key -> line.addView(stat(title(key), display(key, totals.get(key))), weight(i == 0)) }
            results.addView(line, block(10))
        }
        val extra = column()
        totals.entrySet().filter { it.key !in primary }.forEach { (key, value) ->
            extra.addView(detail(title(key), display(key, value)))
        }
        expandable(results, "More totals", extra)
        section("Plans")
        data.getAsJsonArray("groups")?.forEach { entry ->
            val group = entry.asJsonObject
            val body = column().apply { setPadding(dp(14), dp(14), dp(14), dp(14)) }
            body.addView(text(string(group, "name"), 16f, true), block(6))
            body.addView(text("${string(group, "subscriptions")} subscriptions · ${string(group, "customer_count")} customers", 13f).apply { setTextColor(muted) }, block(8))
            val counts = row()
            counts.addView(text("${string(group, "active")} active", 13f, true), LinearLayout.LayoutParams(0, -2, 1f))
            counts.addView(text("${string(group, "paused")} paused", 13f), LinearLayout.LayoutParams(0, -2, 1f))
            body.addView(counts, block(8))
            body.addView(detail("Plan value", money(group.get("plan_value"))))
            body.addView(detail("Paid", money(group.get("paid"))))
            val more = column()
            listOf("cancelled", "ended", "refunded", "delivered").forEach { more.addView(detail(title(it), display(it, group.get(it)))) }
            expandable(body, "Plan details", more)
            results.addView(card(body), block(10))
        }
        section("Customer subscriptions")
        val records = data.getAsJsonArray("records")
        if (records == null || records.size() == 0) results.addView(card(text("No subscriptions match these filters.", 14f).apply { setPadding(dp(16), dp(20), dp(16), dp(20)) }), block(10))
        records?.forEach { entry ->
            val record = entry.asJsonObject
            val fields = record.getAsJsonObject("fields") ?: JsonObject()
            val body = column().apply { setPadding(dp(14), dp(14), dp(14), dp(14)) }
            val header = row()
            header.addView(text(string(record, "customer"), 17f, true), LinearLayout.LayoutParams(0, -2, 1f))
            header.addView(text(string(fields, "status").replaceFirstChar { it.uppercase() }, 12f, true).apply {
                setPadding(dp(8), dp(5), dp(8), dp(5))
                val active = string(fields, "status") == "active"
                val paused = string(fields, "status") == "paused"
                setBackgroundColor(if (active) Color.rgb(234, 245, 240) else if (paused) Color.rgb(255, 245, 222) else androidx.core.content.ContextCompat.getColor(activity, com.svd.svdagencies.R.color.surface_variant))
                setTextColor(if (active) Color.rgb(38, 112, 84) else if (paused) Color.rgb(143, 99, 27) else muted)
            })
            body.addView(header, block(5))
            body.addView(text(string(fields, "plan"), 14f, true), block(5))
            body.addView(text(string(record, "phone"), 13f).apply { setTextColor(muted) }, block(8))
            val done = fields.get("delivered")?.asInt ?: 0
            val left = fields.get("remaining")?.asInt ?: 0
            body.addView(ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = (done + left).coerceAtLeast(1); progress = done
                progressTintList = ColorStateList.valueOf(ink)
                contentDescription = "$done delivered, $left remaining"
            }, LinearLayout.LayoutParams(-1, dp(6)).apply { bottomMargin = dp(7) })
            body.addView(text("$done delivered · $left remaining", 13f), block(8))
            body.addView(detail("Next delivery", string(fields, "next_delivery")))
            body.addView(detail("Paid", money(fields.get("paid"))))
            body.addView(detail("Outstanding", money(fields.get("outstanding"))))
            val full = column()
            fields.entrySet().filter { it.key !in setOf("plan", "status", "delivered", "remaining", "next_delivery", "paid", "outstanding") }.forEach { (key, value) ->
                full.addView(text(title(key), 12f, true).apply { setTextColor(muted); setPadding(0, dp(8), 0, dp(3)) })
                full.addView(text(display(key, value), 14f))
            }
            expandable(body, "View details", full)
            results.addView(card(body), block(12))
        }
        val page = data.get("page")?.asInt ?: 1
        val pages = data.get("pages")?.asInt ?: 1
        val pager = row()
        pager.addView(MaterialButton(activity).apply { styleButton(this, "Previous", false); isEnabled = page > 1; setOnClickListener { onPage(page - 1) } }, LinearLayout.LayoutParams(-2, dp(46)))
        pager.addView(text("$page / $pages", 13f).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(0, -2, 1f))
        pager.addView(MaterialButton(activity).apply { styleButton(this, "Next", false); isEnabled = page < pages; setOnClickListener { onPage(page + 1) } }, LinearLayout.LayoutParams(-2, dp(46)))
        results.addView(pager)
    }

    private fun expandable(parent: LinearLayout, label: String, details: LinearLayout) {
        details.visibility = View.GONE
        val toggle = MaterialButton(activity).apply {
            styleButton(this, label, false); textSize = 13f
            setOnClickListener { details.visibility = if (details.visibility == View.VISIBLE) View.GONE else View.VISIBLE; text = if (details.visibility == View.VISIBLE) "Hide details" else label }
        }
        parent.addView(toggle, LinearLayout.LayoutParams(-1, dp(44)).apply { topMargin = dp(8); bottomMargin = dp(6) })
        parent.addView(details)
    }
    private fun section(value: String) { results.addView(text(value, 19f, true).apply { setPadding(0, dp(14), 0, dp(10)) }) }
    private fun stat(label: String, value: String) = card(column().apply {
        setPadding(dp(12), dp(12), dp(12), dp(12))
        addView(text(label, 12f).apply { setTextColor(muted) }, block(5))
        addView(text(value, 19f, true))
    })
    private fun input(key: String, value: String) = TextInputEditText(activity).apply {
        setText(value); setSingleLine(); textSize = 13f; setPadding(dp(10), 0, dp(10), 0)
        setTextColor(ink); setHintTextColor(muted); setBackgroundResource(R.drawable.bg_qty_outline)
        inputs[key] = this
    }
    private fun field(label: String, control: View) = column().apply {
        addView(text(label, 12f).apply { setTextColor(muted) }, block(4))
        addView(control, LinearLayout.LayoutParams(-1, dp(48)))
    }
    private fun detail(label: String, value: String) = row().apply {
        setPadding(0, dp(4), 0, dp(4))
        addView(text(label, 13f).apply { setTextColor(muted) }, LinearLayout.LayoutParams(0, -2, 1f))
        addView(text(value, 14f, true).apply { gravity = Gravity.END }, LinearLayout.LayoutParams(0, -2, 1f))
    }
    private fun styleButton(button: MaterialButton, label: String, primary: Boolean) {
        button.text = label; button.isAllCaps = false; button.minWidth = 0; button.cornerRadius = dp(10)
        button.strokeWidth = dp(1); button.strokeColor = ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(activity, com.svd.svdagencies.R.color.card_stroke_color))
        button.backgroundTintList = ColorStateList.valueOf(if (primary) androidx.core.content.ContextCompat.getColor(activity, com.svd.svdagencies.R.color.colorPrimary) else androidx.core.content.ContextCompat.getColor(activity, com.svd.svdagencies.R.color.surface_background))
        button.setTextColor(if (primary) androidx.core.content.ContextCompat.getColor(activity, com.svd.svdagencies.R.color.svd_on_primary) else ink); button.iconTint = ColorStateList.valueOf(if (primary) androidx.core.content.ContextCompat.getColor(activity, com.svd.svdagencies.R.color.svd_on_primary) else ink)
        button.setPadding(dp(10), 0, dp(10), 0)
    }
    private fun card(child: View) = MaterialCardView(activity).apply {
        radius = dp(14).toFloat(); cardElevation = 0f; strokeWidth = dp(1); strokeColor = androidx.core.content.ContextCompat.getColor(activity, com.svd.svdagencies.R.color.card_stroke_color); setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(activity, com.svd.svdagencies.R.color.surface_background))
        addView(child, ViewGroup.LayoutParams(-1, -2))
    }
    private fun text(value: String, size: Float, bold: Boolean = false) = TextView(activity).apply {
        text = value; textSize = size; setTextColor(ink); if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun row() = com.svd.svdagencies.utils.CompactLinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun column() = com.svd.svdagencies.utils.CompactLinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
    private fun weight(first: Boolean) = LinearLayout.LayoutParams(0, -2, 1f).apply { if (first) rightMargin = dp(5) else leftMargin = dp(5) }
    private fun block(bottom: Int = 0) = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(bottom) }
    private fun string(data: JsonObject, key: String) = data.get(key)?.takeUnless { it.isJsonNull }?.asString?.takeIf { it.isNotBlank() } ?: "—"
    private fun title(key: String) = key.replace('_', ' ').replaceFirstChar { it.uppercase() }
    private fun money(value: JsonElement?) = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).format(value?.takeUnless { it.isJsonNull }?.asDouble ?: 0.0)
    private fun display(key: String, value: JsonElement?): String {
        if (key in setOf("paid", "net_paid", "outstanding", "refunded", "plan_value", "total")) return money(value)
        if (value == null || value.isJsonNull) return "—"
        if (value.isJsonArray) return value.asJsonArray.joinToString("\n") { display("", it) }
        if (value.isJsonObject) return value.asJsonObject.entrySet().joinToString(" · ") { "${title(it.key)}: ${display(it.key, it.value)}" }
        return value.asString.takeIf { it.isNotBlank() } ?: "—"
    }
    private fun dp(value: Int) = (value * activity.resources.displayMetrics.density).toInt()
}
