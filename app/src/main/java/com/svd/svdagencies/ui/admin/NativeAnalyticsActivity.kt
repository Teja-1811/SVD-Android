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
    private var busy = false
    private var catalogReady = false
    private val controls = mutableListOf<View>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_simple_dashboard)
        setupAdminLayout(if (kind == "offers") "Offer usage" else "Subscriptions")
        findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh).isEnabled = false
        content = findViewById(R.id.contentContainer)
        content.addView(label(if (kind == "offers") "Offer performance and customer orders" else "Plans, funding and deliveries", 22f))
        for ((key, hint) in listOf("from_date" to "From date (YYYY-MM-DD)", "to_date" to "To date (YYYY-MM-DD)", "customer" to "Customer name or phone")) {
            val field = TextInputEditText(this).apply { setSingleLine(); setText(savedInstanceState?.getString(key).orEmpty()) }
            val wrapper = TextInputLayout(this).apply { this.hint = hint; boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE; addView(field) }
            content.addView(wrapper); inputs[key] = field; controls.add(field)
        }
        offer = Spinner(this); status = Spinner(this)
        content.addView(label("Offer / plan")); content.addView(offer)
        content.addView(label("Status")); content.addView(status)
        controls.add(offer); controls.add(status)
        button("Apply filters") { page = 1; load() }
        button("Clear filters") { inputs.values.forEach { it.setText("") }; offer.setSelection(0); status.setSelection(0); page = 1; load() }
        button(if(kind == "offers") "Manage offers" else "Manage subscription offers") {
            startActivity(android.content.Intent(this, if(kind == "offers") com.svd.svdagencies.ui.admin.offers.AdminOffersActivity::class.java else com.svd.svdagencies.ui.admin.offers.AdminSubscriptionOffersActivity::class.java))
        }
        message = label("Loading reportâ€¦"); content.addView(message)
        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; content.addView(results)
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
        if (busy) return
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
                        val names = listOf("All") + rows.map { it.asJsonObject.get("name").asString }
                        spinner.adapter = ArrayAdapter(this@NativeAnalyticsActivity, android.R.layout.simple_spinner_dropdown_item, names)
                        return listOf("") + rows.map { it.asJsonObject.get("id").asString }
                    }
                    offerIds = setup(offer, "catalog"); statusIds = setup(status, "statuses"); catalogReady = true
                }
                results.removeAllViews()
                results.addView(label("Overview", 20f))
                val grid = GridLayout(this@NativeAnalyticsActivity).apply { columnCount = 2 }
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
                    val body = LinearLayout(this@NativeAnalyticsActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(12,12,12,12) }
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
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                message.text = "Could not load the report. Check your connection and try Apply filters again."
            } finally { busy = false; controls.forEach { it.isEnabled = true } }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        inputs.forEach { (key, input) -> outState.putString(key, input.text.toString()) }
        super.onSaveInstanceState(outState)
    }
}
