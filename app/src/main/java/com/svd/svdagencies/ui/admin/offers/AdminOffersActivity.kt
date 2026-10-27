package com.svd.svdagencies.ui.admin.offers

import android.app.DatePickerDialog
import android.os.Bundle
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.admin.*
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import com.svd.svdagencies.utils.NetworkMessageUtils
import com.svd.svdagencies.utils.PortalDates
import com.svd.svdagencies.utils.WhiteMaterialAlertDialogBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AdminOffersActivity : AdminBaseActivity() {
    private lateinit var offersList: LinearLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var catalogue = emptyList<AdminOfferProduct>()
    private var manager: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_offers_dashboard)
        setupAdminLayout("Offer bundles")
        swipeRefresh = findViewById(R.id.swipeRefresh)
        offersList = findViewById(R.id.offersList)
        findViewById<MaterialButton>(R.id.btnCreateOffer).apply {
            text = "Create bundle"
            setOnClickListener { showOfferDialog() }
        }
        swipeRefresh.setOnRefreshListener { loadOffers() }
    }
    override fun onResume() { super.onResume(); loadOffers() }

    private fun loadOffers(openProducts: Int? = null) {
        swipeRefresh.isRefreshing = true
        ApiClient.adminOffersApi.listOffers().enqueue(object : Callback<AdminOffersResponse> {
            override fun onResponse(call: Call<AdminOffersResponse>, response: Response<AdminOffersResponse>) {
                if (isFinishing || isDestroyed) return
                swipeRefresh.isRefreshing = false
                if (!response.isSuccessful) return message("Unable to load bundles")
                catalogue = response.body()?.items.orEmpty()
                val offers = response.body()?.offers.orEmpty()
                offersList.removeAllViews()
                if (offers.isEmpty()) offersList.addView(label("No bundles yet.", 15f))
                offers.forEach(::addOfferRow)
                offers.firstOrNull { it.id == openProducts }?.let(::showBundleItems)
            }
            override fun onFailure(call: Call<AdminOffersResponse>, error: Throwable) {
                if (isFinishing || isDestroyed) return
                swipeRefresh.isRefreshing = false; message("Unable to load bundles")
            }
        })
    }
    private fun addOfferRow(offer: AdminOffer) {
        val body = com.svd.svdagencies.utils.CompactLinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)) }
        body.addView(label("${if (offer.offer_for == "user") "Users" else "Retailers"} · ${if (offer.is_active) "Published" else "Draft"}", 12f))
        body.addView(label(offer.name, 19f, true))
        body.addView(label("${money(offer.price)} for the complete bundle", 17f, true))
        body.addView(label("${offer.start_date} — ${offer.end_date}", 12f))
        if (offer.description.orEmpty().isNotBlank()) body.addView(label(offer.description.orEmpty(), 14f))
        body.addView(label("Included products", 14f, true))
        if (offer.items.orEmpty().isEmpty()) body.addView(label("Add products before publishing.", 14f))
        offer.items.orEmpty().forEach { item -> body.addView(label("${item.item_name} × ${item.buy_qty + item.offer_qty}", 14f)) }
        val first = com.svd.svdagencies.utils.CompactLinearLayout(this)
        first.addView(action("Edit bundle") { showOfferDialog(offer) }, slot(true))
        first.addView(action("Products") { showBundleItems(offer) }, slot(false))
        body.addView(first)
        val second = com.svd.svdagencies.utils.CompactLinearLayout(this)
        second.addView(action(if (offer.is_active) "Unpublish" else "Publish") {
            if (!offer.is_active && offer.items.orEmpty().isEmpty()) message("Add at least one product before publishing.")
            else submit(ApiClient.adminOffersApi.updateOffer(offer.id, mapOf("is_active" to !offer.is_active)), { loadOffers() })
        }, slot(true))
        second.addView(action("Delete bundle") { deleteOffer(offer) }.apply { setTextColor(getColor(R.color.red600)) }, slot(false))
        body.addView(second)
        offersList.addView(MaterialCardView(this).apply {
            radius = dp(14).toFloat(); cardElevation = 0f; strokeWidth = dp(1)
            strokeColor = getColor(R.color.card_stroke_color); setCardBackgroundColor(getColor(R.color.surface_background))
            addView(body)
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
    }

    private fun showOfferDialog(existing: AdminOffer? = null) {
        val form = layoutInflater.inflate(R.layout.admin_dialog_offer_form, null)
        val name = form.findViewById<EditText>(R.id.etOfferName).apply { setText(existing?.name.orEmpty()) }
        val description = form.findViewById<EditText>(R.id.etOfferDescription).apply { setText(existing?.description.orEmpty()) }
        val audience = form.findViewById<Spinner>(R.id.spinnerOfferFor).apply {
            adapter = ArrayAdapter(this@AdminOffersActivity, com.svd.svdagencies.R.layout.delivery_dropdown_item, listOf("Retailers", "Users"))
            setSelection(if (existing?.offer_for == "user") 1 else 0)
        }
        val price = form.findViewById<EditText>(R.id.etOfferPrice).apply { setText(existing?.price?.toString().orEmpty()) }
        val start = form.findViewById<EditText>(R.id.etOfferStartDate).apply { setText(existing?.start_date ?: PortalDates.isoDay()); datePicker(this) }
        val end = form.findViewById<EditText>(R.id.etOfferEndDate).apply { setText(existing?.end_date ?: PortalDates.isoDay(30)); datePicker(this) }
        val dialog = WhiteMaterialAlertDialogBuilder(this).setTitle(if (existing == null) "Create bundle" else "Edit bundle")
            .setView(form).setNegativeButton("Cancel", null).setPositiveButton(if(existing == null) "Create & add products" else "Save", null).create()
        dialog.setOnShowListener {
            val save = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            save.setOnClickListener {
                val total = price.text.toString().toBigDecimalOrNull()
                if (name.text.isBlank()) { name.error = "Enter a bundle name"; return@setOnClickListener }
                if (total == null || total.signum() < 0) { price.error = "Enter one valid bundle price"; return@setOnClickListener }
                val body = mutableMapOf<String, Any>("name" to name.text.toString().trim(), "description" to description.text.toString(),
                    "price" to total.toPlainString(), "offer_for" to if(audience.selectedItemPosition == 1) "user" else "retailer",
                    "offer_type" to "bundle", "start_date" to start.text.toString(), "end_date" to end.text.toString())
                if (existing == null) body["is_active"] = false
                save.isEnabled = false
                val request = if(existing == null) ApiClient.adminOffersApi.createOffer(body) else ApiClient.adminOffersApi.updateOffer(existing.id, body)
                submit(request, { response ->
                    dialog.dismiss()
                    val id = (response["offer"] as? Map<*, *>)?.get("id") as? Number
                    loadOffers(if(existing == null) id?.toInt() else null)
                }, { save.isEnabled = true })
            }
        }
        dialog.show()
    }

    private fun showBundleItems(offer: AdminOffer) {
        manager?.dismiss()
        val body = com.svd.svdagencies.utils.CompactLinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(4), dp(18), dp(8)) }
        body.addView(label("Bundle price: ${money(offer.price)}", 17f, true))
        body.addView(label("Add or remove products and set quantities. The bundle price stays unchanged.", 14f).apply { visibility = android.view.View.GONE })
        offer.items.orEmpty().forEach { item ->
            body.addView(label("${item.item_name} × ${item.buy_qty + item.offer_qty}", 15f, true))
            val actions = com.svd.svdagencies.utils.CompactLinearLayout(this)
            actions.addView(action("Edit quantity") { manager?.dismiss(); showItemForm(offer, item) }, slot(true))
            actions.addView(action("Remove") {
                WhiteMaterialAlertDialogBuilder(this).setTitle("Remove ${item.item_name}?")
                    .setMessage("The bundle price remains ${money(offer.price)}.")
                    .setNegativeButton("Cancel", null).setPositiveButton("Remove") { _, _ ->
                        submit(ApiClient.adminOffersApi.deleteOfferItem(item.id), { loadOffers(offer.id) })
                    }.show()
            }.apply { setTextColor(getColor(R.color.red600)) }, slot(false))
            body.addView(actions)
        }
        if (offer.items.orEmpty().isEmpty()) body.addView(label("No products included.", 14f))
        manager = WhiteMaterialAlertDialogBuilder(this).setTitle(offer.name).setView(ScrollView(this).apply { addView(body) })
            .setNegativeButton("Done", null).setPositiveButton("Add product") { _, _ -> showItemForm(offer) }.show()
    }

    private fun showItemForm(offer: AdminOffer, existing: AdminOfferItem? = null) {
        val included = offer.items.orEmpty().map { it.item_id }.toSet()
        val choices = if(existing == null) catalogue.filter { it.id !in included } else
            listOf(catalogue.firstOrNull { it.id == existing.item_id } ?: AdminOfferProduct(existing.item_id, existing.item_name))
        if (choices.isEmpty()) { message("All available products are already included."); showBundleItems(offer); return }
        val form = layoutInflater.inflate(R.layout.admin_dialog_offer_item_form, null)
        val product = form.findViewById<Spinner>(R.id.spinnerOfferProduct).apply {
            adapter = ArrayAdapter(this@AdminOffersActivity, com.svd.svdagencies.R.layout.delivery_dropdown_item, choices.map { it.name })
            isEnabled = existing == null
        }
        val quantity = form.findViewById<EditText>(R.id.etOfferBuyQuantity).apply { setText((existing?.let { it.buy_qty + it.offer_qty } ?: 1).toString()) }
        form.findViewById<TextView>(R.id.tvBundlePrice).text = "Bundle price: ${money(offer.price)}"
        val dialog = WhiteMaterialAlertDialogBuilder(this).setTitle(if(existing == null) "Add product" else "Edit included quantity")
            .setView(form).setNegativeButton("Cancel") { _, _ -> showBundleItems(offer) }.setPositiveButton("Save product", null).create()
        dialog.setOnShowListener {
            val save = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            save.setOnClickListener {
                val amount = quantity.text.toString().toIntOrNull()
                if (amount == null || amount < 1) { quantity.error = "Enter a positive whole quantity"; return@setOnClickListener }
                save.isEnabled = false
                val request = if(existing == null) ApiClient.adminOffersApi.addOfferItem(offer.id, mapOf("item" to choices[product.selectedItemPosition].id, "quantity" to amount, "buy_qty" to amount, "offer_qty" to 0, "offer_price" to 0))
                    else ApiClient.adminOffersApi.updateOfferItem(existing.id, mapOf("quantity" to amount, "buy_qty" to amount, "offer_qty" to 0, "offer_price" to 0))
                submit(request, { dialog.dismiss(); loadOffers(offer.id) }, { save.isEnabled = true })
            }
        }
        dialog.show()
    }

    private fun datePicker(field: EditText) {
        field.isFocusable = false
        field.setOnClickListener {
            val calendar = Calendar.getInstance()
            runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(field.text.toString()) }.getOrNull()?.let { calendar.time = it }
            DatePickerDialog(this, { _, year, month, day -> field.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)) },
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }
    private fun deleteOffer(offer: AdminOffer) {
        WhiteMaterialAlertDialogBuilder(this).setTitle("Delete ${offer.name}?").setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ -> submit(ApiClient.adminOffersApi.deleteOffer(offer.id), { loadOffers() }) }.show()
    }
    private fun submit(request: Call<Map<String, Any>>, success: (Map<String, Any>) -> Unit, failure: () -> Unit = {}) {
        swipeRefresh.isRefreshing = true
        request.enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (isFinishing || isDestroyed) return
                swipeRefresh.isRefreshing = false
                if (response.isSuccessful) success(response.body().orEmpty())
                else { message(NetworkMessageUtils.parseError(response, "Unable to save bundle")); failure() }
            }
            override fun onFailure(call: Call<Map<String, Any>>, error: Throwable) {
                if (isFinishing || isDestroyed) return
                swipeRefresh.isRefreshing = false; message("Unable to save bundle. Try again."); failure()
            }
        })
    }
    private fun label(value: String, size: Float, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(getColor(R.color.textColorPrimary)); setPadding(0, dp(4), 0, dp(4))
        if(bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun action(value: String, click: () -> Unit) = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
        text = value; isAllCaps = false; textSize = 13f; minWidth = 0; cornerRadius = dp(10); setPadding(dp(8), 0, dp(8), 0)
        setOnClickListener { click() }
    }
    private fun slot(first: Boolean) = LinearLayout.LayoutParams(0, dp(48), 1f).apply { topMargin = dp(6); if(first) rightMargin = dp(4) else leftMargin = dp(4) }
    private fun money(value: Double) = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).format(value)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun message(value: String) = Toast.makeText(this, value, Toast.LENGTH_LONG).show()
}
