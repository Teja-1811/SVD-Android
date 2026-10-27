package com.svd.svdagencies.ui.delivery

import android.app.DatePickerDialog
import android.os.Bundle
import com.svd.svdagencies.utils.DeliveryMaps
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.svd.svdagencies.R
import com.svd.svdagencies.base.BaseActivity
import com.svd.svdagencies.data.api.auth.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DeliverySubscriptionDeliveriesActivity : BaseActivity() {
    private val selectedDate = Calendar.getInstance()
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
    private lateinit var dateButton: MaterialButton
    private lateinit var loading: ProgressBar
    private lateinit var empty: TextView
    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.delivery_subscription_deliveries)
        val toolbar = findViewById<MaterialToolbar>(R.id.subscriptionDeliveryToolbar)
        DeliveryNavigation.setup(
            this,
            findViewById<DrawerLayout>(R.id.deliveryDrawerLayout),
            findViewById<NavigationView>(R.id.deliveryNavigationView),
            toolbar = toolbar,
            selectedItemId = R.id.nav_delivery_subscriptions,
        )
        dateButton = findViewById(R.id.btnSubscriptionDeliveryDate)
        loading = findViewById(R.id.subscriptionDeliveryLoading)
        empty = findViewById(R.id.tvSubscriptionDeliveriesEmpty)
        container = findViewById(R.id.subscriptionDeliveriesContainer)
        dateButton.setOnClickListener { chooseDate() }
        updateDateButton()
        loadDeliveries()
    }

    private fun chooseDate() {
        DatePickerDialog(this, { _, year, month, day ->
            selectedDate.set(year, month, day)
            updateDateButton()
            loadDeliveries()
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun updateDateButton() { dateButton.text = displayDateFormat.format(selectedDate.time) }

    private fun loadDeliveries() {
        loading.isVisible = true
        ApiClient.deliveryApi.getSubscriptionDeliveries(apiDateFormat.format(selectedDate.time)).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                loading.isVisible = false
                if (!response.isSuccessful) return toast("Could not load subscription deliveries.")
                render(response.body()?.getAsJsonArray("deliveries") ?: JsonArray())
            }
            override fun onFailure(call: Call<JsonObject>, error: Throwable) {
                loading.isVisible = false; toast("Could not load subscription deliveries.")
            }
        })
    }

    private fun render(rows: JsonArray) {
        container.removeAllViews()
        empty.isVisible = rows.size() == 0
        rows.forEach { addDeliveryCard(it.asJsonObject) }
    }

    private fun addDeliveryCard(delivery: JsonObject) {
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
            setCardBackgroundColor(ContextCompat.getColor(this@DeliverySubscriptionDeliveriesActivity, R.color.surface_background))
            radius = dp(16).toFloat(); strokeWidth = dp(1)
            strokeColor = ContextCompat.getColor(this@DeliverySubscriptionDeliveriesActivity, R.color.card_stroke_color)
        }
        val body = com.svd.svdagencies.utils.CompactLinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(12)) }
        body.addView(label(delivery.value("customer_name"), 18, true))
        body.addView(label(delivery.value("customer_phone"), 14, false))
        val latitude = delivery.get("delivery_latitude")?.takeUnless { it.isJsonNull }?.asDouble
        val longitude = delivery.get("delivery_longitude")?.takeUnless { it.isJsonNull }?.asDouble
        if (latitude != null && longitude != null) body.addView(button("Open delivery pin in Google Maps") { DeliveryMaps.open(this, latitude, longitude) })
        val routeAddress = listOf(delivery.value("route"), delivery.value("address")).filter { it.isNotBlank() }.joinToString(" · ")
        if (routeAddress.isNotBlank()) body.addView(label(routeAddress, 14, false))
        body.addView(label("${delivery.value("frequency")} · ${delivery.value("status").replaceFirstChar { it.uppercase() }}", 14, true).apply { setPadding(0, dp(8), 0, 0) })
        val items = delivery.getAsJsonArray("items")?.joinToString("\n") { row ->
            val item = row.asJsonObject
            "• ${item.value("name")} × ${item.value("quantity")}" 
        }.orEmpty()
        body.addView(label(items, 14, false).apply { setPadding(0, dp(8), 0, 0) })
        if (delivery.value("status") == "scheduled") {
            val actions = com.svd.svdagencies.utils.CompactLinearLayout(this).apply { gravity = Gravity.END; orientation = LinearLayout.HORIZONTAL }
            val id = delivery.get("id").asInt
            actions.addView(button("Skip") { update(id, false) }.apply { layoutParams = LinearLayout.LayoutParams(0, -2, 1f); backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT); strokeWidth = dp(1); strokeColor = android.content.res.ColorStateList.valueOf(com.google.android.material.color.MaterialColors.getColor(this@DeliverySubscriptionDeliveriesActivity, com.google.android.material.R.attr.colorError, getColor(R.color.brand_red))); setTextColor(com.google.android.material.color.MaterialColors.getColor(this@DeliverySubscriptionDeliveriesActivity, com.google.android.material.R.attr.colorError, getColor(R.color.brand_red))) })
            actions.addView(button("Mark delivered") { update(id, true) }.apply { layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(8) }; setTextColor(com.google.android.material.color.MaterialColors.getColor(this@DeliverySubscriptionDeliveriesActivity, com.google.android.material.R.attr.colorOnPrimary, getColor(R.color.svd_on_primary))) })
            body.addView(actions)
        } else if (delivery.value("handled_by").isNotBlank()) {
            body.addView(label("Handled by: ${delivery.value("handled_by")}", 13, false).apply { setPadding(0, dp(8), 0, 0) })
        }
        card.addView(body)
        container.addView(card)
    }

    private fun update(id: Int, delivered: Boolean) {
        loading.isVisible = true
        val call = if (delivered) ApiClient.deliveryApi.completeSubscriptionDelivery(id) else ApiClient.deliveryApi.skipSubscriptionDelivery(id)
        call.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                loading.isVisible = false
                toast(if (response.isSuccessful) if (delivered) "Delivery marked delivered." else "Delivery skipped." else "Unable to update delivery.")
                if (response.isSuccessful) loadDeliveries()
            }
            override fun onFailure(call: Call<JsonObject>, error: Throwable) { loading.isVisible = false; toast("Unable to update delivery.") }
        })
    }

    private fun JsonObject.value(name: String) = get(name)?.asString ?: ""
    private fun label(value: String, size: Int, bold: Boolean) = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(ContextCompat.getColor(this@DeliverySubscriptionDeliveriesActivity, R.color.textColorPrimary))
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }
    private fun button(label: String, click: () -> Unit) = MaterialButton(this).apply { text = label; isAllCaps = false; setOnClickListener { click() } }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
