package com.svd.svdagencies.ui.customer

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.svd.svdagencies.R
import com.svd.svdagencies.base.BaseActivity
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.api.customer.OrderApi
import com.svd.svdagencies.data.model.customer.*
import com.svd.svdagencies.utils.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Review and order exactly one advertised offer, with its included items. */
class CustomerOfferOrderActivity : BaseActivity() {
    private val deliveryLocation = DeliveryLocationPicker(this) { this }
    private var offer: OfferItem? = null
    private lateinit var details: TextView
    private lateinit var status: TextView
    private lateinit var regular: MaterialButton
    private lateinit var preorder: MaterialButton
    private lateinit var date: MaterialButton
    private lateinit var confirm: MaterialButton
    private lateinit var retry: MaterialButton
    private var isPreorder = false
    private var submitting = false
    private val deliveryDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    private val money = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }
        details = TextView(this).apply { textSize = 18f; text = "Loading offer…" }
        status = TextView(this).apply { textSize = 14f; setPadding(0, 20, 0, 20) }
        root.addView(details); root.addView(status)
        regular = button("Regular order") { setMode(false) }.also { root.addView(it) }
        preorder = button("Pre-order for a date") { setMode(true) }.also { root.addView(it); it.visibility = if (SessionManager(this).getRole() == UserRole.USER) View.VISIBLE else View.GONE }
        date = button("") {
            DatePickerDialog(this, { _, year, month, day ->
                deliveryDate.set(year, month, day); showDate()
            }, deliveryDate.get(Calendar.YEAR), deliveryDate.get(Calendar.MONTH), deliveryDate.get(Calendar.DAY_OF_MONTH)).apply {
                datePicker.minDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
            }.show()
        }.also { root.addView(it) }
        confirm = button("Place offer order") { placeOffer() }.also { root.addView(it); it.isEnabled = false }
        retry = button("Retry loading offer") { loadOffer() }.also { root.addView(it); it.visibility = View.GONE }
        PortalNavigation.installPage(this, ScrollView(this).apply { addView(root) }, "Order offer", R.id.nav_home)
        loadOffer()
    }

    private fun loadOffer() {
        CustomerOrderWindow.refresh {
            if (!isFinishing && !isDestroyed) status.text = CustomerOrderWindow.statusMessage(if (isPreorder) "pre_order" else "regular")
        }
        retry.visibility = View.GONE
        ApiClient.customerApi.getOffers().enqueue(object : Callback<CustomerOfferResponse> {
            override fun onResponse(call: Call<CustomerOfferResponse>, response: Response<CustomerOfferResponse>) {
                if (isFinishing || isDestroyed) return
                val chosen = response.body()?.takeIf { response.isSuccessful && it.status }?.offers?.firstOrNull { it.id == intent.getIntExtra("offer_id", -1) }
                if (chosen == null || chosen.items.isEmpty()) { unavailable(); return }
                offer = chosen
                val charge = if (SessionManager(this@CustomerOfferOrderActivity).getRole() == UserRole.USER) 10.0 else 0.0
                details.text = "${chosen.name}\n\n" + chosen.items.joinToString("\n") {
                    "${it.itemName} × ${it.orderQuantity}" + if (it.offerQty > 0) " (${it.offerQty} free)" else ""
                } + "\n\nOffer price: ${money.format(chosen.price)}\nDelivery: ${money.format(charge)}\nTotal: ${money.format(chosen.price + charge)}"
                regular.isEnabled = chosen.items.all { it.stock >= it.orderQuantity }
                status.text = (if (regular.isEnabled) "Review the included items, then choose delivery." else "Some included items are out of stock. Choose a dated pre-order.") + "\n" + CustomerOrderWindow.statusMessage(if (isPreorder) "pre_order" else "regular")
                val isUser = SessionManager(this@CustomerOfferOrderActivity).getRole() == UserRole.USER
                setMode(!regular.isEnabled && isUser)
                confirm.isEnabled = regular.isEnabled || isUser
            }
            override fun onFailure(call: Call<CustomerOfferResponse>, error: Throwable) {
                if (!isFinishing && !isDestroyed) unavailable()
            }
        })
    }

    private fun unavailable() {
        offer = null
        details.text = "This offer is unavailable or could not be loaded."
        confirm.isEnabled = false
        retry.visibility = View.VISIBLE
    }

    private fun setMode(pre: Boolean) {
        isPreorder = pre
        date.visibility = if (pre) View.VISIBLE else View.GONE
        regular.text = if (!pre) "✓ Regular order" else "Regular order"
        preorder.text = if (pre) "✓ Pre-order for a date" else "Pre-order for a date"
        showDate()
        status.text = CustomerOrderWindow.statusMessage(if (isPreorder) "pre_order" else "regular")
    }

    private fun showDate() { date.text = "Delivery: ${SimpleDateFormat("dd MMM yyyy", Locale.US).format(deliveryDate.time)}" }

    private fun placeOffer(location: DeliveryCoordinates? = null) {
        val chosen = offer ?: return
        if (submitting) return
        if (!isPreorder && !CustomerOrderWindow.isOpen("regular")) { message(CustomerOrderWindow.statusMessage(if (isPreorder) "pre_order" else "regular")); return }
        if (SessionManager(this).getRole() == UserRole.USER && location == null) {
            deliveryLocation.choose { placeOffer(it) }; return
        }
        submitting = true
        confirm.isEnabled = false
        val request = PlaceOrderRequest(
            items = chosen.items.map { OrderItemRequest(it.itemId, it.orderQuantity.toDouble()) },
            orderType = if (isPreorder) "pre_order" else "regular",
            deliveryDate = if (isPreorder) SimpleDateFormat("yyyy-MM-dd", Locale.US).format(deliveryDate.time) else null,
            deliveryLatitude = location?.latitude, deliveryLongitude = location?.longitude,
            offerId = chosen.id, offerPrice = chosen.price,
        )
        ApiClient.retrofit.create(OrderApi::class.java).placeOrder(request).enqueue(object : Callback<PlaceOrderResponse> {
            override fun onResponse(call: Call<PlaceOrderResponse>, response: Response<PlaceOrderResponse>) {
                if (isFinishing || isDestroyed) return
                submitting = false
                confirm.isEnabled = true
                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()
                    if (SessionManager(this@CustomerOfferOrderActivity).getRole() == UserRole.USER && body?.paymentFor == "ORDER" && body.orderId != null && body.grandTotal != null) {
                        startActivity(PortalNavigation.homeIntent(this@CustomerOfferOrderActivity)
                            .putExtra("user_tab", R.id.nav_payment)
                            .putExtra("order_payment_id", body.orderId)
                            .putExtra("order_payment_amount", body.grandTotal))
                    }
                    message("Offer order placed: ${body?.orderNumber.orEmpty()}"); finish()
                } else {
                    val error = runCatching { JsonParser.parseString(response.errorBody()?.string()).asJsonObject.get("message").asString }.getOrNull()
                    message(error ?: response.body()?.message ?: "Could not place this offer order. Please retry.")
                }
            }
            override fun onFailure(call: Call<PlaceOrderResponse>, error: Throwable) {
                if (isFinishing || isDestroyed) return
                submitting = false; confirm.isEnabled = true
                message(NetworkMessageUtils.friendlyMessage(error, "Could not place this offer order"))
            }
        })
    }

    private fun button(label: String, action: () -> Unit) = MaterialButton(this).apply {
        text = label; isAllCaps = false; setOnClickListener { action() }
    }
    private fun message(value: String) = Toast.makeText(this, value, Toast.LENGTH_LONG).show()
}
