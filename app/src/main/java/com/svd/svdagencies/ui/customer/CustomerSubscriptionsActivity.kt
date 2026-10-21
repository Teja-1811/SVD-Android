package com.svd.svdagencies.ui.customer

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import com.svd.svdagencies.utils.DeliveryLocationPicker
import com.svd.svdagencies.utils.DeliveryMaps
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.ScrollView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.textfield.TextInputEditText
import java.text.NumberFormat
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
    private val deliveryLocation = DeliveryLocationPicker(this) { this }
    private var creatingSubscription = false
    private lateinit var loading: ProgressBar
    private lateinit var empty: TextView
    private lateinit var plans: LinearLayout
    private lateinit var container: LinearLayout
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(if (SessionManager(this).getRole() == UserRole.USER) R.layout.user_portal else R.layout.customer)

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

        val screen = layoutInflater.inflate(if (SessionManager(this).getRole() == UserRole.USER) R.layout.user_subscriptions else R.layout.customer_subscriptions, findViewById(R.id.customerFragmentContainer), false)
        findViewById<FrameLayout>(R.id.customerFragmentContainer).addView(screen)
        loading = screen.findViewById(R.id.subscriptionsLoading)
        empty = screen.findViewById(R.id.tvSubscriptionsEmpty)
        container = screen.findViewById(R.id.subscriptionsContainer)
        plans = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        (container.parent as LinearLayout).addView(plans)
        if (SessionManager(this).getRole() == UserRole.USER) {
            findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.customerBottomNav).apply {
                visibility = View.VISIBLE
                menu.clear()
                inflateMenu(R.menu.menu_user_bottom)
                selectedItemId = R.id.nav_subscriptions
                setOnItemSelectedListener { item ->
                    if (item.itemId != R.id.nav_subscriptions) {
                        startActivity(Intent(this@CustomerSubscriptionsActivity, UserPortalActivity::class.java).putExtra("user_tab", item.itemId))
                        finish()
                    }
                    true
                }
            }
        }
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
        empty.isVisible = false
        val active = rows.filter { it.asJsonObject.string("status") != "cancelled" }
        container.isVisible = active.isNotEmpty()
        if (active.isNotEmpty()) container.addView(text("Active subscriptions", 20, true))
        active.forEach { element -> addSubscriptionCard(element.asJsonObject) }
        loadPlans()
    }

    private fun loadPlans() {
        plans.removeAllViews()
        plans.addView(text("Subscription plans", 22, true).apply { setPadding(0, if (container.isVisible) dp(24) else 0, 0, dp(16)) })
        ApiClient.subscriptionApi.getSubscriptionOffers().enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (isFinishing || isDestroyed) return
                if (!response.isSuccessful) {
                    plans.addView(actionButton("Retry loading plans") { loadPlans() }); return
                }
                val offers = response.body()?.getAsJsonArray("offers") ?: JsonArray()
                if (offers.size() == 0) plans.addView(text("No plans available right now.", 14, false))
                offers.forEach { row ->
                    val offer = row.asJsonObject
                    val body = offerDetails(offer).apply {
                        setPadding(dp(20), dp(20), dp(20), dp(16))
                        addView(actionButton("Buy") { showOfferStartForm(offer) }.apply {
                            cornerRadius = dp(12)
                            minHeight = dp(48)
                            contentDescription = "Buy ${offer.string("name")}, ${offerMoney(offer, "total_amount")} total"
                            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) }
                        })
                    }
                    plans.addView(MaterialCardView(this@CustomerSubscriptionsActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) }
                        radius = dp(16).toFloat()
                        cardElevation = 0f
                        strokeWidth = dp(1)
                        strokeColor = ContextCompat.getColor(this@CustomerSubscriptionsActivity, R.color.card_stroke_color)
                        setCardBackgroundColor(ContextCompat.getColor(this@CustomerSubscriptionsActivity, R.color.white))
                        addView(body)
                    })
                }
            }
            override fun onFailure(call: Call<JsonObject>, error: Throwable) {
                if (!isFinishing && !isDestroyed) plans.addView(actionButton("Retry loading plans") { loadPlans() })
            }
        })
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
        body.addView(text(subscription.string("offer_name"), 18, true))
        val latitude = subscription.get("delivery_latitude")?.takeUnless { it.isJsonNull }?.asDouble
        val longitude = subscription.get("delivery_longitude")?.takeUnless { it.isJsonNull }?.asDouble
        if (latitude != null && longitude != null) body.addView(actionButton("Delivery location") { DeliveryMaps.open(this, latitude, longitude) })
        val total = subscription.get("delivery_count")?.asInt ?: 0
        val completed = subscription.get("completed_deliveries")?.asInt ?: 0
        val remaining = subscription.get("remaining_deliveries")?.asInt ?: (total - completed).coerceAtLeast(0)
        body.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = total.coerceAtLeast(1); progress = completed
            contentDescription = "$completed of $total deliveries completed"
            layoutParams = LinearLayout.LayoutParams(-1, dp(12)).apply { topMargin = dp(12) }
        })
        body.addView(text("$completed deliveries done · $remaining remaining", 14, true))
        if (subscription.string("status") == "paused") {
            body.addView(text(subscription.string("resume_on").takeIf { it.isNotBlank() }?.let { "Paused · resumes on $it" } ?: "Paused until you resume", 14, false))
        }
        body.addView(text("Next delivery: ${subscription.string("next_delivery_date")}", 14, false))
        val products = subscription.getAsJsonArray("items")?.joinToString("\n") { item ->
            val row = item.asJsonObject
            "• ${row.string("name")} × ${row.string("quantity")}"
        }.orEmpty()
        body.addView(text(products, 14, false).apply { setPadding(0, dp(8), 0, 0) })
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
        val id = subscription.get("id").asInt
        when (subscription.string("status")) {
            "active" -> actions.addView(actionButton("Pause") { showPauseOptions(id) })
            "paused" -> actions.addView(actionButton("Resume") { updateSubscription(id, "resume") })
        }
        if (subscription.string("status") != "cancelled") {
            actions.addView(actionButton("Cancel") { confirmCancel(subscription) }.apply {
                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this@CustomerSubscriptionsActivity, R.color.white))
                setTextColor(ContextCompat.getColor(this@CustomerSubscriptionsActivity, R.color.brand_red))
                strokeColor = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this@CustomerSubscriptionsActivity, R.color.brand_red))
                strokeWidth = dp(1)
                contentDescription = "Cancel subscription"
                layoutParams = LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) }
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

    private fun offerMoney(offer: JsonObject, key: String): String =
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(offer.get(key)?.takeUnless { it.isJsonNull }?.asDouble ?: 0.0)

    private fun offerDetails(offer: JsonObject): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(text(offer.string("name"), 20, true))
        addView(text(offerMoney(offer, "total_amount"), 30, true).apply {
            setPadding(0, dp(12), 0, dp(2))
        })
        addView(text("${offerMoney(offer, "per_delivery_amount")} / delivery", 13, false).apply {
            setTextColor(ContextCompat.getColor(this@CustomerSubscriptionsActivity, R.color.textColorSecondary))
        })
        val duration = offer.string("duration_value")
        val unit = offer.string("duration_unit").removeSuffix("s") + if (duration == "1") "" else "s"
        val frequency = when (offer.string("delivery_frequency")) {
            "daily" -> "Daily"
            "weekly" -> "Weekly"
            "monthly" -> "Monthly"
            else -> offer.string("delivery_frequency")
        }
        addView(LinearLayout(this@CustomerSubscriptionsActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#F5F7F8"))
                cornerRadius = dp(10).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16); bottomMargin = dp(12) }
            listOf("$duration $unit", frequency, "${offer.string("delivery_count")} deliveries").forEach { label ->
                addView(text(label, 12, true).apply {
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                })
            }
        })
        offer.getAsJsonArray("items")?.forEach { value ->
            val item = value.asJsonObject
            val quantity = NumberFormat.getNumberInstance(Locale("en", "IN")).format(item.get("quantity").asDouble)
            addView(LinearLayout(this@CustomerSubscriptionsActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(8))
                addView(text(item.string("name"), 14, false).apply {
                    layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                })
                addView(text("× $quantity", 14, true).apply { setPadding(dp(12), 0, 0, 0) })
            })
        }
    }

    private fun showOfferPicker(offers: JsonArray) {
        if (offers.size() == 0) return showMessage("No subscription offers are available right now.")
        val choices = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(8))
        }
        val dialog = WhiteMaterialAlertDialogBuilder(this)
            .setTitle("Choose your subscription")
            .setView(ScrollView(this).apply { addView(choices) })
            .setNegativeButton("Not now", null)
            .create()
        offers.forEach { value ->
            val offer = value.asJsonObject
            val details = offerDetails(offer).apply { setPadding(dp(16), dp(16), dp(16), dp(12)) }
            details.addView(actionButton("Buy") {
                dialog.dismiss()
                showOfferStartForm(offer)
            }.apply {
                contentDescription = "Buy ${offer.string("name")}, ${offerMoney(offer, "total_amount")} total"
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
            })
            choices.addView(MaterialCardView(this).apply {
                radius = dp(14).toFloat()
                cardElevation = 0f
                strokeWidth = dp(1)
                strokeColor = android.graphics.Color.parseColor("#DCE3E6")
                setCardBackgroundColor(android.graphics.Color.WHITE)
                addView(details)
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
            })
        }
        dialog.show()
    }

    private fun showOfferStartForm(offer: JsonObject) {
        val sheet = BottomSheetDialog(this)
        val screen = layoutInflater.inflate(R.layout.sheet_subscription_setup, null)
        sheet.setContentView(screen)
        screen.findViewById<TextView>(R.id.subPlanName).text = offer.string("name")
        val duration = offer.string("duration_value")
        val unit = offer.string("duration_unit").removeSuffix("s") + if (duration == "1") "" else "s"
        val frequency = when (offer.string("delivery_frequency")) {
            "daily" -> "Every day"
            "weekly" -> "Once a week"
            "monthly" -> "Once a month"
            else -> offer.string("delivery_frequency")
        }
        screen.findViewById<TextView>(R.id.subSchedule).text = "$frequency · $duration $unit · ${offer.string("delivery_count")} deliveries"
        screen.findViewById<TextView>(R.id.subPlanPrice).text = offerMoney(offer, "total_amount")
        screen.findViewById<TextView>(R.id.subPerDelivery).text = "${offerMoney(offer, "per_delivery_amount")} per delivery · No delivery charge"
        screen.findViewById<TextView>(R.id.subFooterPrice).text = "Plan total: ${offerMoney(offer, "total_amount")}"
        val products = screen.findViewById<LinearLayout>(R.id.subProductRows)
        offer.getAsJsonArray("items")?.forEach { value ->
            val item = value.asJsonObject
            val quantity = NumberFormat.getNumberInstance(Locale("en", "IN")).format(item.get("quantity").asDouble)
            products.addView(LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(10), 0, dp(10))
                addView(text(item.string("name"), 14, false), LinearLayout.LayoutParams(0, -2, 1f))
                addView(text("× $quantity", 15, true).apply { setPadding(dp(12), 0, 0, 0) })
            })
        }
        val startDate = Calendar.getInstance()
        val displayDate = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
        val dateButton = screen.findViewById<MaterialButton>(R.id.subStartDate)
        dateButton.text = displayDate.format(startDate.time) + "  ·  Change"
        dateButton.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                startDate.set(year, month, day)
                dateButton.text = displayDate.format(startDate.time) + "  ·  Change"
            }, startDate.get(Calendar.YEAR), startDate.get(Calendar.MONTH), startDate.get(Calendar.DAY_OF_MONTH)).show()
        }
        val address = screen.findViewById<TextInputEditText>(R.id.subAddress)
        if (SessionManager(this).getRole() == UserRole.USER) {
            (address.parent.parent as? View)?.visibility = View.GONE
            screen.findViewById<TextView>(R.id.subDeliveryLabel).text = "Choose your delivery pin when you confirm. Subscription delivery is free."
        }
        screen.findViewById<View>(R.id.subClose).setOnClickListener { sheet.dismiss() }
        screen.findViewById<MaterialButton>(R.id.subConfirm).setOnClickListener {
            sheet.dismiss()
            createSubscription(JsonObject().apply {
                addProperty("offer_id", offer.get("id").asInt)
                addProperty("start_date", dateFormat.format(startDate.time))
                addProperty("delivery_address", address.text?.toString()?.trim().orEmpty())
            })
        }
        sheet.setOnShowListener {
            val panel = sheet.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            panel?.let {
                it.layoutParams.height = (resources.displayMetrics.heightPixels * 0.9).toInt()
                it.background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.WHITE)
                    cornerRadii = floatArrayOf(dp(24).toFloat(), dp(24).toFloat(), dp(24).toFloat(), dp(24).toFloat(), 0f, 0f, 0f, 0f)
                }
                BottomSheetBehavior.from(it).apply { state = BottomSheetBehavior.STATE_EXPANDED; skipCollapsed = true }
                it.requestLayout()
            }
        }
        sheet.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        sheet.show()
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
        if (SessionManager(this).getRole() == UserRole.USER && !request.has("delivery_latitude")) {
            request.remove("delivery_address")
            deliveryLocation.choose { point ->
                request.addProperty("delivery_latitude", point.latitude)
                request.addProperty("delivery_longitude", point.longitude)
                createSubscription(request)
            }
            return
        }
        if (creatingSubscription) return
        creatingSubscription = true
        loading.isVisible = true
        ApiClient.subscriptionApi.createSubscription(request).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (isFinishing || isDestroyed) return
                creatingSubscription = false
                loading.isVisible = false
                val subscription = response.body()?.getAsJsonObject("subscription")
                if (response.isSuccessful && response.body()?.get("status")?.asBoolean == true && subscription != null) {
                    showMessage("Subscription purchased using wallet.")
                    loadSubscriptions()
                } else {
                    val error = runCatching { com.google.gson.JsonParser.parseString(response.errorBody()?.string()).asJsonObject }.getOrNull()
                    if (error?.get("code")?.asString == "insufficient_wallet") {
                        WalletTopUpDialog.show(this@CustomerSubscriptionsActivity,
                            error.get("required_amount").asDouble, error.get("top_up_amount").asDouble)
                    } else showMessage(error?.string("message") ?: response.body()?.string("message") ?: "Unable to confirm subscription.")
                }
            }
            override fun onFailure(call: Call<JsonObject>, error: Throwable) {
                if (isFinishing || isDestroyed) return
                creatingSubscription = false
                loading.isVisible = false
                showMessage("Unable to confirm subscription. Please check your subscriptions before retrying.")
            }
        })
    }

    private fun showPauseOptions(id: Int) {
        WhiteMaterialAlertDialogBuilder(this).setTitle("Pause deliveries")
            .setItems(arrayOf("For a number of days", "Until I resume")) { _, choice ->
                if (choice == 1) updateSubscription(id, "pause") else {
                    val input = EditText(this).apply { hint = "Number of days"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }
                    val dialog = WhiteMaterialAlertDialogBuilder(this).setTitle("Pause for how many days?")
                        .setView(input).setNegativeButton("Cancel", null).setPositiveButton("Pause", null).create()
                    dialog.setOnShowListener {
                        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val days = input.text.toString().toIntOrNull()
                            if (days == null || days !in 1..3650) input.error = "Enter 1 to 3650 days"
                            else { dialog.dismiss(); updateSubscription(id, "pause", days) }
                        }
                    }
                    dialog.show()
                }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun updateSubscription(id: Int, action: String, days: Int? = null) {
        loading.isVisible = true
        val request = JsonObject().apply { days?.let { addProperty("days", it) } }
        val call = when (action) {
            "pause" -> ApiClient.subscriptionApi.pauseSubscription(id, request)
            "resume" -> ApiClient.subscriptionApi.resumeSubscription(id, request)
            else -> ApiClient.subscriptionApi.cancelSubscription(id, request)
        }
        call.enqueue(subscriptionCallback(when (action) { "pause" -> "Subscription paused."; "resume" -> "Subscription resumed."; else -> "Subscription cancelled." }))
    }

    private fun confirmCancel(subscription: JsonObject) {
        val id = subscription.get("id").asInt
        val remaining = subscription.get("remaining_deliveries")?.asInt ?: 0
        val amount = subscription.get("refund_amount")?.takeUnless { it.isJsonNull }?.asDouble ?: 0.0
        val money = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amount)
        val message = if (amount > 0.0) {
            "$remaining deliveries remaining.\n\nRemaining amount: $money\n\nThis amount will be added to your wallet instantly when you confirm cancellation.\n\nFuture deliveries will stop. Do you want to cancel?"
        } else {
            "Remaining amount: $money. There are no verified unused funds to credit now. If payment is awaiting verification, eligible funds will be credited instantly once verified.\n\nFuture deliveries will stop. Do you want to cancel?"
        }
        WhiteMaterialAlertDialogBuilder(this).setTitle("Cancel subscription?")
            .setMessage(message)
            .setNegativeButton("Keep", null)
            .setPositiveButton("Cancel subscription") { _, _ -> updateSubscription(id, "cancel") }
            .show()
    }

    private fun subscriptionCallback(success: String) = object : Callback<JsonObject> {
        override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
            loading.isVisible = false
            if (response.isSuccessful && response.body()?.get("status")?.asBoolean == true) {
                showMessage(response.body()?.string("message")?.takeIf { it.isNotBlank() } ?: success); loadSubscriptions()
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
    private fun JsonObject.string(name: String) = get(name)?.takeUnless { it.isJsonNull }?.asString ?: ""
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun showMessage(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        PortalNavigation.configure(this, R.id.nav_subscriptions)
    }

}
