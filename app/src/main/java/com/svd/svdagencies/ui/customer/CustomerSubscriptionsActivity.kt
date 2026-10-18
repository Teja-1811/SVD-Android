package com.svd.svdagencies.ui.customer

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.svd.svdagencies.utils.WhiteMaterialAlertDialogBuilder
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.svd.svdagencies.R
import com.svd.svdagencies.base.BaseActivity
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.api.customer.ProductApi
import com.svd.svdagencies.data.api.customer.SubscriptionApi
import com.svd.svdagencies.utils.SessionManager
import com.svd.svdagencies.utils.UserRole
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** User-controlled recurring delivery plans: create, pause, resume and cancel. */
class CustomerSubscriptionsActivity : BaseActivity() {
    private lateinit var loading: ProgressBar
    private lateinit var empty: TextView
    private lateinit var container: LinearLayout
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.customer)

        findViewById<TextView>(R.id.tvCustomerToolbarTitle).text = "My Subscriptions"
        findViewById<View>(R.id.customerBottomNav).visibility = View.GONE
        val drawer = findViewById<DrawerLayout>(R.id.customerDrawerLayout)
        findViewById<View>(R.id.btnCustomerMenu).setOnClickListener { drawer.openDrawer(GravityCompat.START) }
        findViewById<NavigationView>(R.id.customerNavigationView).apply {
            setCheckedItem(R.id.nav_subscriptions)
            getHeaderView(0).findViewById<View>(R.id.btnCloseDrawer).setOnClickListener { drawer.closeDrawer(GravityCompat.START) }
            setNavigationItemSelectedListener { item ->
                if (item.itemId == R.id.nav_home) {
                    val portal = if (SessionManager(this@CustomerSubscriptionsActivity).getRole() == UserRole.USER) {
                        UserPortalActivity::class.java
                    } else CustomerMainActivity::class.java
                    startActivity(Intent(this@CustomerSubscriptionsActivity, portal))
                    finish()
                    true
                } else if (item.itemId == R.id.nav_subscriptions) {
                    drawer.closeDrawer(GravityCompat.START); true
                } else false
            }
        }
        findViewById<View>(R.id.btnCustomerLogout).setOnClickListener {
            SessionManager(this).logout()
            startActivity(Intent(this, com.svd.svdagencies.ui.auth.LoginActivity::class.java))
            finish()
        }

        val screen = layoutInflater.inflate(R.layout.customer_subscriptions, findViewById(R.id.customerFragmentContainer), false)
        findViewById<FrameLayout>(R.id.customerFragmentContainer).addView(screen)
        loading = screen.findViewById(R.id.subscriptionsLoading)
        empty = screen.findViewById(R.id.tvSubscriptionsEmpty)
        container = screen.findViewById(R.id.subscriptionsContainer)
        screen.findViewById<MaterialButton>(R.id.btnCreateSubscription).setOnClickListener { selectProducts() }
        loadSubscriptions()
    }

    private fun loadSubscriptions() {
        loading.isVisible = true
        ApiClient.subscriptionApi.getSubscriptions().enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                loading.isVisible = false
                if (!response.isSuccessful) return showMessage("Could not load subscriptions.")
                showSubscriptions(response.body()?.getAsJsonArray("subscriptions") ?: JsonArray())
            }
            override fun onFailure(call: Call<JsonObject>, error: Throwable) {
                loading.isVisible = false
                showMessage("Could not load subscriptions. Please try again.")
            }
        })
    }

    private fun showSubscriptions(rows: JsonArray) {
        container.removeAllViews()
        empty.isVisible = rows.size() == 0
        rows.forEach { element -> addSubscriptionCard(element.asJsonObject) }
    }

    private fun addSubscriptionCard(subscription: JsonObject) {
        val status = subscription.string("status").replaceFirstChar { it.uppercase() }
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
            setCardBackgroundColor(ContextCompat.getColor(this@CustomerSubscriptionsActivity, R.color.white))
            radius = dp(16).toFloat()
            strokeWidth = dp(1)
            strokeColor = ContextCompat.getColor(this@CustomerSubscriptionsActivity, R.color.card_stroke_color)
        }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        body.addView(text("$status · ${subscription.string("frequency_label")}", 18, true))
        body.addView(text("Next delivery: ${subscription.string("next_delivery_date")}", 14, false))
        val products = subscription.getAsJsonArray("items")?.joinToString("\n") { item ->
            val row = item.asJsonObject
            "• ${row.string("name")} × ${row.string("quantity")}" 
        }.orEmpty()
        body.addView(text(products, 14, false).apply { setPadding(0, dp(8), 0, 0) })
        val upcoming = subscription.getAsJsonArray("upcoming_deliveries")?.joinToString(", ") { it.asString }.orEmpty()
        if (upcoming.isNotBlank()) body.addView(text("Upcoming: $upcoming", 13, false).apply { setPadding(0, dp(8), 0, 0) })
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
        val id = subscription.get("id").asInt
        when (subscription.string("status")) {
            "active" -> actions.addView(actionButton("Pause") { updateSubscription(id, "pause") })
            "paused" -> actions.addView(actionButton("Resume") { updateSubscription(id, "resume") })
        }
        if (subscription.string("status") != "cancelled") {
            actions.addView(actionButton("Cancel") { confirmCancel(id) }.apply {
                setTextColor(ContextCompat.getColor(this@CustomerSubscriptionsActivity, R.color.brand_red))
            })
        }
        body.addView(actions)
        card.addView(body)
        container.addView(card)
    }

    private fun selectProducts() {
        loading.isVisible = true
        ApiClient.subscriptionApi.getSubscriptionOffers().enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                loading.isVisible = false
                if (!response.isSuccessful) return showMessage("Could not load subscription offers.")
                showOfferPicker(response.body()?.getAsJsonArray("offers") ?: JsonArray())
            }
            override fun onFailure(call: Call<JsonObject>, error: Throwable) {
                loading.isVisible = false; showMessage("Could not load subscription offers.")
            }
        })
    }

    private fun showOfferPicker(offers: JsonArray) {
        if (offers.size() == 0) return showMessage("No subscription offers are available right now.")
        val labels = offers.map { offer ->
            val row = offer.asJsonObject
            "${row.string("name")} · ₹${row.string("total_amount")}\n${row.string("delivery_count")} deliveries over ${row.string("duration_value")} ${row.string("duration_unit")}" 
        }.toTypedArray()
        WhiteMaterialAlertDialogBuilder(this)
            .setTitle("Choose a subscription offer")
            .setItems(labels) { _, index -> showOfferStartForm(offers[index].asJsonObject) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showOfferStartForm(offer: JsonObject) {
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22), 0, dp(22), 0) }
        form.addView(text("${offer.string("name")} · ₹${offer.string("total_amount")}", 17, true))
        val items = offer.getAsJsonArray("items")?.joinToString("\n") { row ->
            val item = row.asJsonObject
            "• ${item.string("name")} × ${item.string("quantity")}" 
        }.orEmpty()
        form.addView(text(items, 14, false).apply { setPadding(0, dp(8), 0, 0) })
        val startDate = Calendar.getInstance()
        val dateButton = MaterialButton(this).apply { text = "Starts: ${dateFormat.format(startDate.time)}" }
        dateButton.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                startDate.set(year, month, day)
                dateButton.text = "Starts: ${dateFormat.format(startDate.time)}"
            }, startDate.get(Calendar.YEAR), startDate.get(Calendar.MONTH), startDate.get(Calendar.DAY_OF_MONTH)).show()
        }
        form.addView(dateButton)
        val address = EditText(this).apply { hint = "Delivery address (optional)"; minLines = 2 }
        form.addView(address)
        WhiteMaterialAlertDialogBuilder(this)
            .setTitle("Start subscription")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Subscribe") { _, _ ->
                createSubscription(JsonObject().apply {
                    addProperty("offer_id", offer.get("id").asInt)
                    addProperty("start_date", dateFormat.format(startDate.time))
                    addProperty("delivery_address", address.text.toString().trim())
                })
            }.show()
    }

    private fun loadCategoryProducts(categories: JsonArray, index: Int, products: MutableList<JsonObject>) {
        if (index >= categories.size()) {
            loading.isVisible = false
            showProductPicker(products)
            return
        }
        val category = categories[index].asJsonObject
        val categoryId = category.get("id")?.asInt ?: return loadCategoryProducts(categories, index + 1, products)
        ApiClient.productApi.getProducts(categoryId).enqueue(object : Callback<com.google.gson.JsonElement> {
            override fun onResponse(call: Call<com.google.gson.JsonElement>, response: Response<com.google.gson.JsonElement>) {
                val rows = response.body()?.asJsonObject?.getAsJsonArray("products") ?: JsonArray()
                rows.forEach { row -> products.add(row.asJsonObject) }
                loadCategoryProducts(categories, index + 1, products)
            }
            override fun onFailure(call: Call<com.google.gson.JsonElement>, error: Throwable) {
                loadCategoryProducts(categories, index + 1, products)
            }
        })
    }

    private fun showProductPicker(products: List<JsonObject>) {
        if (products.isEmpty()) return showMessage("No products are available for subscriptions right now.")
        val selected = BooleanArray(products.size)
        WhiteMaterialAlertDialogBuilder(this)
            .setTitle("Choose products")
            .setMultiChoiceItems(products.map { "${it.string("name")}  ·  ₹${it.string("selling_price")}" }.toTypedArray(), selected) { _, which, checked -> selected[which] = checked }
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Continue") { _, _ ->
                val chosen = products.filterIndexed { index, _ -> selected[index] }
                if (chosen.isEmpty()) showMessage("Choose at least one product.") else showSubscriptionForm(chosen)
            }.show()
    }

    private fun showSubscriptionForm(products: List<JsonObject>) {
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22), 0, dp(22), 0) }
        form.addView(text("Set the quantity for each delivery", 14, false))
        val quantities = mutableMapOf<Int, EditText>()
        products.forEach { product ->
            form.addView(text(product.string("name"), 14, true).apply { setPadding(0, dp(12), 0, 0) })
            quantities[product.get("id").asInt] = EditText(this).apply {
                hint = "Quantity"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setText("1")
            }
            form.addView(quantities.getValue(product.get("id").asInt))
        }
        form.addView(text("Delivery frequency", 14, true).apply { setPadding(0, dp(12), 0, 0) })
        val frequency = Spinner(this).apply {
            setPopupBackgroundResource(R.drawable.bg_delivery_dropdown)
            adapter = ArrayAdapter(this@CustomerSubscriptionsActivity, R.layout.delivery_dropdown_item, arrayOf("Every day", "Alternate days", "Every week"))
        }
        form.addView(frequency)
        val startDate = Calendar.getInstance()
        val dateButton = MaterialButton(this).apply { text = "Starts: ${dateFormat.format(startDate.time)}" }
        dateButton.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                startDate.set(year, month, day)
                dateButton.text = "Starts: ${dateFormat.format(startDate.time)}"
            }, startDate.get(Calendar.YEAR), startDate.get(Calendar.MONTH), startDate.get(Calendar.DAY_OF_MONTH)).show()
        }
        form.addView(dateButton)
        val address = EditText(this).apply { hint = "Delivery address (optional)"; minLines = 2 }
        form.addView(address)
        WhiteMaterialAlertDialogBuilder(this)
            .setTitle("New recurring delivery")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Start") { _, _ ->
                val items = JsonArray()
                products.forEach { product ->
                    val quantity = quantities[product.get("id").asInt]?.text.toString().toIntOrNull() ?: 0
                    if (quantity > 0) items.add(JsonObject().apply { addProperty("item_id", product.get("id").asInt); addProperty("quantity", quantity) })
                }
                if (items.size() == 0) return@setPositiveButton showMessage("Enter a quantity for at least one product.")
                val values = arrayOf("daily", "alternate_days", "weekly")
                createSubscription(JsonObject().apply {
                    add("items", items)
                    addProperty("frequency", values[frequency.selectedItemPosition])
                    addProperty("start_date", dateFormat.format(startDate.time))
                    addProperty("delivery_address", address.text.toString().trim())
                })
            }.show()
    }

    private fun createSubscription(request: JsonObject) {
        loading.isVisible = true
        ApiClient.subscriptionApi.createSubscription(request).enqueue(subscriptionCallback("Subscription started."))
    }

    private fun updateSubscription(id: Int, action: String) {
        loading.isVisible = true
        val request = JsonObject()
        val call = when (action) {
            "pause" -> ApiClient.subscriptionApi.pauseSubscription(id, request)
            "resume" -> ApiClient.subscriptionApi.resumeSubscription(id, request)
            else -> ApiClient.subscriptionApi.cancelSubscription(id, request)
        }
        call.enqueue(subscriptionCallback("Subscription ${action}d."))
    }

    private fun confirmCancel(id: Int) {
        WhiteMaterialAlertDialogBuilder(this).setTitle("Cancel subscription?")
            .setMessage("Future recurring deliveries will stop.")
            .setNegativeButton("Keep", null)
            .setPositiveButton("Cancel subscription") { _, _ -> updateSubscription(id, "cancel") }
            .show()
    }

    private fun subscriptionCallback(success: String) = object : Callback<JsonObject> {
        override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
            loading.isVisible = false
            if (response.isSuccessful && response.body()?.get("status")?.asBoolean == true) {
                showMessage(success); loadSubscriptions()
            } else showMessage(response.body()?.string("message") ?: "Unable to update subscription.")
        }
        override fun onFailure(call: Call<JsonObject>, error: Throwable) {
            loading.isVisible = false; showMessage("Unable to update subscription.")
        }
    }

    private fun text(value: String, size: Int, bold: Boolean) = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(ContextCompat.getColor(this@CustomerSubscriptionsActivity, R.color.textColorPrimary))
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }
    private fun actionButton(label: String, onClick: () -> Unit) = MaterialButton(this).apply {
        text = label; isAllCaps = false; setOnClickListener { onClick() }
    }
    private fun JsonObject.string(name: String) = get(name)?.asString ?: ""
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun showMessage(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
