package com.svd.svdagencies.ui.admin.offers

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.svd.svdagencies.utils.WhiteMaterialAlertDialogBuilder
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/** Admin UI for fixed, User-only subscription plans. */
class AdminSubscriptionOffersActivity : AdminBaseActivity() {
    private lateinit var list: LinearLayout
    private lateinit var loading: ProgressBar
    private var catalogueItems = JsonArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_subscription_offers_dashboard)
        findViewById<MaterialToolbar>(R.id.subscriptionOfferToolbar).setNavigationOnClickListener { finish() }
        list = findViewById(R.id.subscriptionOffersList)
        loading = findViewById(R.id.subscriptionOffersLoading)
        findViewById<MaterialButton>(R.id.btnCreateSubscriptionOffer).setOnClickListener { showCreateDialog() }
        loadOffers()
    }

    private fun loadOffers() {
        loading.isVisible = true
        ApiClient.adminOffersApi.listSubscriptionOffers().enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                loading.isVisible = false
                if (!response.isSuccessful) return toast("Unable to load subscription offers")
                val body = response.body() ?: return
                catalogueItems = body.getAsJsonArray("items") ?: JsonArray()
                list.removeAllViews()
                (body.getAsJsonArray("offers") ?: JsonArray()).forEach { addOfferCard(it.asJsonObject) }
            }
            override fun onFailure(call: Call<JsonObject>, error: Throwable) { loading.isVisible = false; toast("Unable to load subscription offers") }
        })
    }

    private fun addOfferCard(offer: JsonObject) {
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
            setCardBackgroundColor(ContextCompat.getColor(this@AdminSubscriptionOffersActivity, R.color.white))
            radius = dp(16).toFloat(); strokeWidth = dp(1); strokeColor = ContextCompat.getColor(this@AdminSubscriptionOffersActivity, R.color.card_stroke_color)
        }
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(14)) }
        body.addView(label(offer.value("name"), 18, true))
        body.addView(label("User Portal · ${offer.value("duration_value")} ${offer.value("duration_unit")} · ${offer.value("delivery_frequency")}", 14, false))
        body.addView(label("${offer.value("delivery_count")} deliveries · ₹${offer.value("total_amount")}", 15, true).apply { setPadding(0, dp(6), 0, 0) })
        val itemLines = offer.getAsJsonArray("items")?.joinToString("\n") { item ->
            val row = item.asJsonObject
            "• ${row.value("name")} × ${row.value("quantity")} @ ₹${row.value("unit_price")}" 
        }.orEmpty()
        body.addView(label(itemLines, 13, false).apply { setPadding(0, dp(8), 0, 0) })
        body.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(MaterialButton(this@AdminSubscriptionOffersActivity).apply {
                text = "Edit"; isAllCaps = false
                setOnClickListener { showEditDialog(offer) }
            })
        })
        card.addView(body); list.addView(card)
    }

    private fun showCreateDialog() {
        if (catalogueItems.size() == 0) return toast("No active items are available.")
        val form = layoutInflater.inflate(R.layout.admin_dialog_subscription_offer_form, null)
        val name = form.findViewById<EditText>(R.id.etSubscriptionOfferName)
        val description = form.findViewById<EditText>(R.id.etSubscriptionOfferDescription)
        val duration = form.findViewById<EditText>(R.id.etSubscriptionDuration).apply { setText("1") }
        val durationUnit = form.findViewById<Spinner>(R.id.spinnerSubscriptionDurationUnit)
        val frequency = form.findViewById<Spinner>(R.id.spinnerSubscriptionFrequency)
        val itemSpinner = form.findViewById<Spinner>(R.id.spinnerSubscriptionItem)
        val quantity = form.findViewById<EditText>(R.id.etSubscriptionItemQuantity).apply { setText("1") }
        val price = form.findViewById<EditText>(R.id.etSubscriptionItemPrice)
        durationUnit.adapter = ArrayAdapter(this, R.layout.delivery_dropdown_item, arrayOf("days", "weeks", "months"))
        frequency.adapter = ArrayAdapter(this, R.layout.delivery_dropdown_item, arrayOf("daily", "weekly", "monthly"))
        val itemLabels = catalogueItems.map { item -> "${item.asJsonObject.value("name")} · ₹${item.asJsonObject.value("selling_price")}" }.toTypedArray()
        itemSpinner.adapter = ArrayAdapter(this, R.layout.delivery_dropdown_item, itemLabels)
        itemSpinner.setSelection(0)
        price.setText(catalogueItems[0].asJsonObject.value("selling_price"))
        WhiteMaterialAlertDialogBuilder(this).setTitle("Create User Subscription Offer").setView(form).setNegativeButton("Cancel", null)
            .setPositiveButton("Create") { _, _ ->
                val selectedItem = catalogueItems[itemSpinner.selectedItemPosition].asJsonObject
                val item = mapOf(
                    "item_id" to selectedItem.get("id").asInt,
                    "quantity_per_delivery" to (quantity.text.toString().toIntOrNull() ?: 0),
                    "unit_price" to (price.text.toString().toDoubleOrNull() ?: -1.0),
                )
                val body = mapOf<String, Any>(
                    "name" to name.text.toString().trim(), "description" to description.text.toString().trim(),
                    "duration_value" to (duration.text.toString().toIntOrNull() ?: 0),
                    "duration_unit" to durationUnit.selectedItem.toString(),
                    "delivery_frequency" to frequency.selectedItem.toString(),
                    "items" to listOf(item),
                )
                create(body)
            }.show()
    }

    private fun create(body: Map<String, Any>) {
        loading.isVisible = true
        ApiClient.adminOffersApi.createSubscriptionOffer(body).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                loading.isVisible = false
                if (response.isSuccessful) { toast("Subscription offer created"); loadOffers() } else toast("Unable to create subscription offer")
            }
            override fun onFailure(call: Call<JsonObject>, error: Throwable) { loading.isVisible = false; toast("Unable to create subscription offer") }
        })
    }

    private fun showEditDialog(offer: JsonObject) {
        val form = layoutInflater.inflate(R.layout.admin_dialog_subscription_offer_form, null)
        val name = form.findViewById<EditText>(R.id.etSubscriptionOfferName).apply { setText(offer.value("name")) }
        val description = form.findViewById<EditText>(R.id.etSubscriptionOfferDescription).apply { setText(offer.value("description")) }
        val duration = form.findViewById<EditText>(R.id.etSubscriptionDuration).apply { setText(offer.value("duration_value")) }
        val durationUnit = form.findViewById<Spinner>(R.id.spinnerSubscriptionDurationUnit)
        val frequency = form.findViewById<Spinner>(R.id.spinnerSubscriptionFrequency)
        durationUnit.adapter = ArrayAdapter(this, R.layout.delivery_dropdown_item, arrayOf("days", "weeks", "months"))
        frequency.adapter = ArrayAdapter(this, R.layout.delivery_dropdown_item, arrayOf("daily", "weekly", "monthly"))
        durationUnit.setSelection(arrayOf("days", "weeks", "months").indexOf(offer.value("duration_unit")).coerceAtLeast(0))
        frequency.setSelection(arrayOf("daily", "weekly", "monthly").indexOf(offer.value("delivery_frequency")).coerceAtLeast(0))

        // Item rows are fixed by the plan. Editing plan metadata from Android
        // keeps every saved item and price intact; the website supports full
        // multi-item editing.
        form.findViewById<Spinner>(R.id.spinnerSubscriptionItem).visibility = View.GONE
        form.findViewById<EditText>(R.id.etSubscriptionItemQuantity).visibility = View.GONE
        form.findViewById<EditText>(R.id.etSubscriptionItemPrice).visibility = View.GONE

        WhiteMaterialAlertDialogBuilder(this).setTitle("Edit Subscription Offer").setView(form).setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val body = mapOf<String, Any>(
                    "name" to name.text.toString().trim(),
                    "description" to description.text.toString().trim(),
                    "duration_value" to (duration.text.toString().toIntOrNull() ?: 0),
                    "duration_unit" to durationUnit.selectedItem.toString(),
                    "delivery_frequency" to frequency.selectedItem.toString(),
                )
                update(offer.get("id").asInt, body)
            }.show()
    }

    private fun update(id: Int, body: Map<String, Any>) {
        loading.isVisible = true
        ApiClient.adminOffersApi.updateSubscriptionOffer(id, body).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                loading.isVisible = false
                if (response.isSuccessful) { toast("Subscription offer updated"); loadOffers() } else toast("Unable to update subscription offer")
            }
            override fun onFailure(call: Call<JsonObject>, error: Throwable) { loading.isVisible = false; toast("Unable to update subscription offer") }
        })
    }

    private fun JsonObject.value(name: String) = get(name)?.asString ?: ""
    private fun label(value: String, size: Int, bold: Boolean) = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(ContextCompat.getColor(this@AdminSubscriptionOffersActivity, R.color.textColorPrimary))
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun toast(value: String) = Toast.makeText(this, value, Toast.LENGTH_SHORT).show()
}
