package com.svd.svdagencies.ui.admin.offers

import android.os.Bundle
import android.content.Intent
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.admin.AdminOffer
import com.svd.svdagencies.data.api.admin.AdminOffersResponse
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDate

class AdminOffersActivity : AdminBaseActivity() {
    private lateinit var offersList: LinearLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_offers_dashboard)
        setupAdminLayout("Offers")
        swipeRefresh = findViewById(R.id.swipeRefresh)
        offersList = findViewById(R.id.offersList)
        findViewById<MaterialButton>(R.id.btnCreateOffer).setOnClickListener { showOfferDialog() }
        findViewById<MaterialButton>(R.id.btnSubscriptionOffers).setOnClickListener {
            startActivity(Intent(this, AdminSubscriptionOffersActivity::class.java))
        }
        swipeRefresh.setOnRefreshListener { loadOffers() }
        loadOffers()
    }

    override fun onResume() { super.onResume(); loadOffers() }

    private fun loadOffers() = ApiClient.adminOffersApi.listOffers().enqueue(object : Callback<AdminOffersResponse> {
        override fun onResponse(call: Call<AdminOffersResponse>, response: Response<AdminOffersResponse>) {
            swipeRefresh.isRefreshing = false
            if (!response.isSuccessful) return message("Unable to load offers")
            offersList.removeAllViews(); response.body()?.offers.orEmpty().forEach(::addOfferRow)
        }
        override fun onFailure(call: Call<AdminOffersResponse>, error: Throwable) { swipeRefresh.isRefreshing = false; message("Unable to load offers") }
    })

    private fun addOfferRow(offer: AdminOffer) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 18, 0, 18) }
        row.addView(TextView(this).apply { text = "${offer.name}\n₹${offer.price} • ${offer.start_date} to ${offer.end_date}" }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Button(this).apply { text = "Edit"; setOnClickListener { showOfferDialog(offer) } })
        row.addView(Button(this).apply { text = "Item"; setOnClickListener { showOfferItemDialog(offer) } })
        row.addView(Button(this).apply { text = if (offer.is_active) "Disable" else "Enable"; setOnClickListener { saveOffer(offer, mapOf("is_active" to !offer.is_active)) } })
        row.addView(Button(this).apply { text = "Delete"; setOnClickListener { deleteOffer(offer) } })
        offersList.addView(row, ViewGroup.LayoutParams(-1, -2))
    }

    private fun showOfferDialog(existing: AdminOffer? = null) {
        val form = layoutInflater.inflate(R.layout.admin_dialog_offer_form, null)
        val name = form.findViewById<EditText>(R.id.etOfferName).apply { setText(existing?.name.orEmpty()) }
        val description = form.findViewById<EditText>(R.id.etOfferDescription).apply { setText(existing?.description.orEmpty()) }
        val offerFor = form.findViewById<Spinner>(R.id.spinnerOfferFor).apply {
            adapter = ArrayAdapter(this@AdminOffersActivity, android.R.layout.simple_spinner_dropdown_item, arrayOf("retailer", "user"))
            setSelection(if (existing?.offer_for == "user") 1 else 0)
        }
        val price = form.findViewById<EditText>(R.id.etOfferPrice).apply { setText(existing?.price?.toString().orEmpty()) }
        val start = form.findViewById<EditText>(R.id.etOfferStartDate).apply { setText(existing?.start_date ?: LocalDate.now().toString()) }
        val end = form.findViewById<EditText>(R.id.etOfferEndDate).apply { setText(existing?.end_date ?: LocalDate.now().plusDays(30).toString()) }
        AlertDialog.Builder(this).setTitle(if (existing == null) "Create offer" else "Edit offer").setView(form).setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val body = mapOf<String, Any>("name" to name.text.toString(), "description" to description.text.toString(), "price" to (price.text.toString().toDoubleOrNull() ?: 0.0), "offer_for" to offerFor.selectedItem.toString(), "offer_type" to (existing?.offer_type ?: "bundle"), "start_date" to start.text.toString(), "end_date" to end.text.toString())
                if (existing == null) createOffer(body) else saveOffer(existing, body)
            }.show()
    }

    private fun createOffer(body: Map<String, Any>) = ApiClient.adminOffersApi.createOffer(body).enqueue(reloadCallback())

    private fun showOfferItemDialog(offer: AdminOffer) {
        val form = layoutInflater.inflate(R.layout.admin_dialog_offer_item_form, null)
        val itemId = form.findViewById<EditText>(R.id.etOfferItemId)
        val buyQty = form.findViewById<EditText>(R.id.etOfferBuyQuantity).apply { setText("1") }
        val freeQty = form.findViewById<EditText>(R.id.etOfferFreeQuantity).apply { setText("0") }
        val price = form.findViewById<EditText>(R.id.etOfferItemPrice).apply { setText("0") }
        AlertDialog.Builder(this).setTitle("Add offer item").setView(form).setNegativeButton("Cancel", null)
            .setPositiveButton("Add") { _, _ ->
                val body = mapOf<String, Any>("item" to (itemId.text.toString().toIntOrNull() ?: 0), "buy_qty" to (buyQty.text.toString().toIntOrNull() ?: 0), "offer_qty" to (freeQty.text.toString().toIntOrNull() ?: 0), "offer_price" to (price.text.toString().toDoubleOrNull() ?: 0.0))
                ApiClient.adminOffersApi.addOfferItem(offer.id, body).enqueue(reloadCallback())
            }.show()
    }

    private fun saveOffer(offer: AdminOffer, body: Map<String, Any>) = ApiClient.adminOffersApi.updateOffer(offer.id, body).enqueue(reloadCallback())
    private fun deleteOffer(offer: AdminOffer) = AlertDialog.Builder(this).setMessage("Delete ${offer.name}?").setNegativeButton("Cancel", null).setPositiveButton("Delete") { _, _ -> ApiClient.adminOffersApi.deleteOffer(offer.id).enqueue(reloadCallback()) }.show()
    private fun reloadCallback() = object : Callback<Map<String, Any>> { override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) { if (response.isSuccessful) loadOffers() else message("Unable to save offer") }; override fun onFailure(call: Call<Map<String, Any>>, error: Throwable) = message("Unable to save offer") }
    private fun message(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}
