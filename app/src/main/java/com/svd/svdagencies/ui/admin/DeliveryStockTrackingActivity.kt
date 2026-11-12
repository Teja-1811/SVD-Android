package com.svd.svdagencies.ui.admin

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.delivery.DeliveryStockTrackingResponse
import com.svd.svdagencies.utils.AppSwipeRefreshLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DeliveryStockTrackingActivity : AdminBaseActivity() {
    private lateinit var content: LinearLayout
    private lateinit var agent: Spinner
    private lateinit var dateButton: MaterialButton
    private lateinit var billRemaining: MaterialButton
    private lateinit var refresh: AppSwipeRefreshLayout
    private val start = Calendar.getInstance()
    private var agents = emptyList<com.svd.svdagencies.data.model.delivery.DeliveryStockTrackingAgent>()
    private var applyingAgentSelection = false
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun date(calendar: Calendar) = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_delivery_stock_tracking)
        setupAdminLayout("Delivery Stock Tracking")
        content = findViewById(R.id.trackingContent)
        refresh = findViewById(R.id.swipeRefresh)
        renderFilters()
        refresh.setOnRefreshListener { load() }
        load()
    }

    private fun text(value: String, size: Float = 14f, bold: Boolean = false) = TextView(this).apply {
        this.text = value; textSize = size; setTextColor(getColor(if (bold) R.color.textColorPrimary else R.color.textColorSecondary))
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }
    private fun card(): LinearLayout {
        val card = MaterialCardView(this).apply { radius = dp(14).toFloat(); strokeWidth = dp(1); setStrokeColor(getColor(R.color.card_stroke_color)); cardElevation = 0f
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) } }
        return LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(12)); card.addView(this); content.addView(card) }
    }
    private fun renderFilters() {
        val panel = card()
        panel.addView(text("Today's delivery stock", 17f, true))
        agent = Spinner(this).apply { minimumHeight = dp(48); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) } }
        panel.addView(agent)
        dateButton = MaterialButton(this).apply { isAllCaps = false; layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) } }
        billRemaining = MaterialButton(this).apply {
            text = "Generate bill for remaining stock"; isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }
            setOnClickListener { generateBillForRemaining() }
        }
        panel.addView(dateButton); panel.addView(billRemaining)
        dateButton.setOnClickListener { pick(start) }; updateDateButtons()
    }
    private fun pick(target: Calendar) = DatePickerDialog(this, { _, year, month, day -> target.set(year, month, day); updateDateButtons(); load() }, target.get(Calendar.YEAR), target.get(Calendar.MONTH), target.get(Calendar.DAY_OF_MONTH)).show()
    private fun updateDateButtons() { dateButton.text = "Date ${date(start)}" }

    private fun generateBillForRemaining() {
        val selected = agent.selectedItemPosition.takeIf { it > 0 }?.let { agents.getOrNull(it - 1)?.id }
        if (selected == null) {
            android.widget.Toast.makeText(this, "Select one delivery agent before billing", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Generate remaining-stock bill?")
            .setMessage("This creates one bill for the selected agent's current remaining stock.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Generate") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val result = ApiClient.billsDashboardApi.billRemainingDeliveryStock(selected, date(start))
                        android.widget.Toast.makeText(this@DeliveryStockTrackingActivity,
                            "Bill ${result["invoice_number"] ?: "created"}", android.widget.Toast.LENGTH_LONG).show()
                        load()
                    } catch (error: Exception) {
                        android.widget.Toast.makeText(this@DeliveryStockTrackingActivity,
                            "Could not generate a bill. Check remaining stock and try again.", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }.show()
    }
    private fun load() {
        lifecycleScope.launch {
            try {
                val selected = agent.selectedItemPosition.takeIf { it > 0 }?.let { agents.getOrNull(it - 1)?.id }
                val result = ApiClient.billsDashboardApi.getDeliveryStockTracking(selected, date(start), date(start))
                render(result)
            } catch (error: Exception) {
                content.addView(text("Could not load delivery stock tracking. Pull down to try again.").apply { setPadding(0, dp(12), 0, dp(12)) })
            } finally { refresh.isRefreshing = false }
        }
    }
    private fun render(result: DeliveryStockTrackingResponse) {
        agents = result.agents
        val previous = agent.selectedItemPosition.coerceAtLeast(0)
        applyingAgentSelection = true
        agent.adapter = ArrayAdapter(this, R.layout.delivery_dropdown_item, listOf("All delivery agents") + agents.map { it.name })
        agent.setSelection(previous.coerceAtMost(agents.size), false)
        applyingAgentSelection = false
        agent.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) { if (!applyingAgentSelection && view != null) load() }
        }
        while (content.childCount > 1) content.removeViewAt(1)
        val totals = card()
        totals.addView(text("Reconciliation summary", 17f, true))
        totals.addView(text("Taken ${result.totals["taken"] ?: 0.0}   •   Billed ${result.totals["billed"] ?: 0.0}\nReturned ${result.totals["returned"] ?: 0.0}   •   Difference ${result.totals["difference"] ?: 0.0}").apply { setPadding(0, dp(8), 0, 0) })
        totals.addView(text("${result.unmatchedCount} item rows need review").apply { setPadding(0, dp(6), 0, 0) })
        if (result.rows.isEmpty()) { content.addView(text("No stock entries or bills found for these filters.").apply { gravity = Gravity.CENTER; setPadding(0, dp(32), 0, dp(32)) }); return }
        result.rows.forEach { row ->
            card().apply {
                addView(text(row.itemName, 16f, true)); addView(text("${row.agentName}${row.itemCode?.let { " · $it" } ?: ""}").apply { setPadding(0, dp(2), 0, dp(8)) })
                addView(text("Morning: taken ${row.morningTaken}, returned ${row.morningReturned}\nEvening: taken ${row.eveningTaken}, returned ${row.eveningReturned}\nTotal taken ${row.taken} · Billed ${row.billed} · Returned ${row.returned}"))
                addView(text("Difference: ${row.difference}", 15f, true).apply { setTextColor(getColor(if (row.difference == 0.0) R.color.icon_green else R.color.red600)); setPadding(0, dp(8), 0, 0) })
            }
        }
    }
}
