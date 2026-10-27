package com.svd.svdagencies.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.QueryMap

interface NativeAnalyticsApi {
    @GET("api/admin/analytics/{kind}/")
    suspend fun report(@Path("kind") kind: String, @QueryMap filters: Map<String, String>): JsonObject
}
class AdminOfferUsageActivity : NativeAnalyticsActivity()
class AdminSubscriptionsReportActivity : NativeAnalyticsActivity()

open class NativeAnalyticsActivity : AdminBaseActivity() {
    private val kind get() = if (this is AdminOfferUsageActivity) "offers" else "subscriptions"
    private val api by lazy { ApiClient.retrofit.create(NativeAnalyticsApi::class.java) }
    private lateinit var content: LinearLayout
    private lateinit var results: LinearLayout
    private lateinit var message: TextView
    private lateinit var offer: Spinner
    private lateinit var status: Spinner
    private val inputs = linkedMapOf<String, TextInputEditText>()
    private var offerIds = listOf("")
    private var statusIds = listOf("")
    private var page = 1
    private var subscriptionUi: SubscriptionDashboardUi? = null
    private var refreshPending = false
    private var busy = false
    private var catalogReady = false
    private val controls = mutableListOf<View>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_simple_dashboard)
        setupAdminLayout(if (kind == "offers") "Offer usage" else "Subscriptions")
        findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh).isEnabled = false
        content = findViewById(R.id.contentContainer)
        if (kind == "subscriptions") {
            val ui = SubscriptionDashboardUi(this, content, savedInstanceState)
            subscriptionUi = ui
            offer = ui.plan; status = ui.status
            inputs.putAll(ui.inputs)
            message = ui.message; results = ui.results
            controls.addAll(inputs.values); controls.addAll(listOf(offer, status, ui.refresh, ui.manage))
            inputs.filterKeys { it != "customer" }.values.forEach { field ->
                field.setOnClickListener {
                    val calendar = java.util.Calendar.getInstance()
                    runCatching { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(field.text.toString()) }.getOrNull()?.let { calendar.time = it }
                    android.app.DatePickerDialog(this, { _, year, month, day ->
                        field.setText(String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, day))
                    }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show()
                }
            }
            ui.refresh.setOnClickListener {
                inputs.values.forEach { it.setText("") }; offer.setSelection(0); status.setSelection(0)
                ui.syncFilters(); page = 1; load()
            }
            ui.manage.setOnClickListener { startActivity(android.content.Intent(this, com.svd.svdagencies.ui.admin.offers.AdminSubscriptionOffersActivity::class.java)) }
        } else {
            val panel = layoutInflater.inflate(R.layout.admin_offers_content, content, false)
            content.addView(panel)
            inputs["from_date"] = panel.findViewById(R.id.analyticsFrom)
            inputs["to_date"] = panel.findViewById(R.id.analyticsTo)
            inputs["customer"] = panel.findViewById(R.id.analyticsCustomer)
            inputs.filterKeys { it != "customer" }.values.forEach { field ->
                field.setOnClickListener {
                    val calendar = java.util.Calendar.getInstance()
                    android.app.DatePickerDialog(this, { _, year, month, day ->
                        field.setText(String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, day))
                    }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show()
                }
            }
            offer = panel.findViewById(R.id.analyticsPlan)
            status = panel.findViewById(R.id.analyticsStatus)
            message = panel.findViewById(R.id.analyticsMessage)
            results = panel.findViewById(R.id.analyticsResults)
            controls.addAll(inputs.values); controls.add(offer); controls.add(status)
            panel.findViewById<MaterialButton>(R.id.analyticsApply).apply { setOnClickListener { page = 1; load() }; controls.add(this) }
            panel.findViewById<MaterialButton>(R.id.analyticsRefresh).apply {
                setOnClickListener { inputs.values.forEach { it.setText("") }; offer.setSelection(0); status.setSelection(0); page = 1; load() }
                controls.add(this)
            }
            panel.findViewById<MaterialButton>(R.id.analyticsManage).apply {
                setOnClickListener { startActivity(android.content.Intent(this@NativeAnalyticsActivity, com.svd.svdagencies.ui.admin.offers.AdminOffersActivity::class.java)) }
                controls.add(this)
            }
        }
        load()
    }
    private fun label(value: String, size: Float = 14f) = TextView(this).apply {
        text = value; textSize = size; setTextColor(getColor(R.color.textColorPrimary)); setPadding(12, 16, 12, 16)
    }
    private fun button(title: String, action: () -> Unit) {
        val button = MaterialButton(this).apply { text = title; setOnClickListener { action() } }
        controls.add(button); content.addView(button)
    }
    private fun value(element: JsonElement?): String = when {
        element == null || element.isJsonNull -> "â€”"
        element.isJsonArray -> element.asJsonArray.joinToString("\n") { value(it) }
        element.isJsonObject -> element.asJsonObject.entrySet().joinToString("\n") { "${title(it.key)}: ${value(it.value)}" }
        else -> element.asString
    }
    private fun title(key: String) = key.replace('_', ' ').replaceFirstChar { it.uppercase() }
    private fun load() {
        if (busy) { if (kind == "subscriptions") refreshPending = true; return }
        busy = true; controls.forEach { it.isEnabled = false }; message.text = "Loading reportâ€¦"
        val filters = inputs.mapValues { it.value.text.toString().trim() }.toMutableMap()
        filters["offer"] = offerIds.getOrElse(offer.selectedItemPosition) { "" }
        filters["status"] = statusIds.getOrElse(status.selectedItemPosition) { "" }
        filters["page"] = page.toString()
        lifecycleScope.launch {
            try {
                val data = api.report(kind, filters)
                if (!catalogReady) {
                    fun setup(spinner: Spinner, key: String): List<String> {
                        val rows = data.getAsJsonArray(key)
                        val names = listOf(if(kind == "subscriptions") { if(key == "catalog") "All plans" else "All statuses" } else "All") + rows.map { it.asJsonObject.get("name").asString }
                        spinner.adapter = ArrayAdapter(this@NativeAnalyticsActivity, com.svd.svdagencies.R.layout.delivery_dropdown_item, names)
                        return listOf("") + rows.map { it.asJsonObject.get("id").asString }
                    }
                    offerIds = setup(offer, "catalog"); statusIds = setup(status, "statuses"); catalogReady = true
                    subscriptionUi?.bindAutomaticFilters { page = 1; load() }
                }
                if (kind == "subscriptions") {
                    page = data.get("page")?.asInt ?: 1
                    subscriptionUi?.render(data) { next -> page = next; load() }
                    message.text = data.get("error")?.asString?.takeIf { it.isNotBlank() } ?: "Showing matching subscriptions"
                } else {
                results.removeAllViews()
                results.addView(label("Overview", 20f))
                val grid = GridLayout(this@NativeAnalyticsActivity).apply { columnCount = resources.getInteger(R.integer.svd_summary_columns) }
                data.getAsJsonObject("totals").entrySet().forEach { row ->
                    grid.addView(label("${title(row.key)}\n${value(row.value)}", 16f).apply {
                        layoutParams = GridLayout.LayoutParams().apply { width = 0; columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f) }
                    })
                }
                results.addView(grid)
                results.addView(label(if(kind == "offers") "Usage by offer" else "Usage by plan", 20f))
                data.getAsJsonArray("groups").forEach { entry ->
                    results.addView(label(value(entry)))
                }
                results.addView(label(if(kind == "offers") "Customer offer orders" else "Customer subscriptions", 20f))
                val records = data.getAsJsonArray("records")
                if (records.size() == 0) results.addView(label("No records match these filters."))
                records.forEach { entry ->
                    val row = entry.asJsonObject
                    val body = com.svd.svdagencies.utils.CompactLinearLayout(this@NativeAnalyticsActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(12,12,12,12) }
                    body.addView(label("${row.get("customer").asString}\n${row.get("phone").asString}", 17f))
                    val details = label(value(row.get("fields"))).apply { visibility = View.GONE }
                    body.addView(MaterialButton(this@NativeAnalyticsActivity).apply { text = "View details"; setOnClickListener { details.visibility = if(details.visibility == View.VISIBLE) View.GONE else View.VISIBLE } })
                    body.addView(details)
                    results.addView(MaterialCardView(this@NativeAnalyticsActivity).apply { addView(body) })
                }
                val pages = data.get("pages").asInt; page = data.get("page").asInt
                results.addView(label("Page $page of $pages"))
                results.addView(MaterialButton(this@NativeAnalyticsActivity).apply { text = "Previous"; isEnabled = page > 1; setOnClickListener { page--; load() } })
                results.addView(MaterialButton(this@NativeAnalyticsActivity).apply { text = "Next"; isEnabled = page < pages; setOnClickListener { page++; load() } })
                message.text = data.get("error").asString.ifBlank { "Report updated" }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                message.text = if(kind == "subscriptions") "Could not load subscriptions. Check your connection and tap refresh." else "Could not load the report. Check your connection and try Apply filters again."
            } finally { busy = false; controls.forEach { it.isEnabled = true }; if(refreshPending) { refreshPending = false; load() } }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        inputs.forEach { (key, input) -> outState.putString(key, input.text.toString()) }
        super.onSaveInstanceState(outState)
    }
}
