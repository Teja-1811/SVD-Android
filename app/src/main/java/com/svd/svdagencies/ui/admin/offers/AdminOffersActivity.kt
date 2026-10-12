package com.svd.svdagencies.ui.admin.offers

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 32, 32, 32) }
        root.addView(Button(this).apply { text = "Create offer"; setOnClickListener { showOfferDialog() } })
        offersList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(offersList) }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root); loadOffers()
    }

    override fun onResume() { super.onResume(); loadOffers() }

    private fun loadOffers() = ApiClient.adminOffersApi.listOffers().enqueue(object : Callback<AdminOffersResponse> {
        override fun onResponse(call: Call<AdminOffersResponse>, response: Response<AdminOffersResponse>) {
            if (!response.isSuccessful) return message("Unable to load offers")
            offersList.removeAllViews(); response.body()?.offers.orEmpty().forEach(::addOfferRow)
        }
        override fun onFailure(call: Call<AdminOffersResponse>, error: Throwable) = message("Unable to load offers")
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
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 16, 48, 0) }
        val name = EditText(this).apply { hint = "Name"; setText(existing?.name.orEmpty()) }
        val description = EditText(this).apply { hint = "Description"; setText(existing?.description.orEmpty()) }
        val price = EditText(this).apply { hint = "Offer price"; setText(existing?.price?.toString().orEmpty()) }
        val start = EditText(this).apply { hint = "Start date (YYYY-MM-DD)"; setText(existing?.start_date ?: LocalDate.now().toString()) }
        val end = EditText(this).apply { hint = "End date (YYYY-MM-DD)"; setText(existing?.end_date ?: LocalDate.now().plusDays(30).toString()) }
        listOf(name, description, price, start, end).forEach(form::addView)
        AlertDialog.Builder(this).setTitle(if (existing == null) "Create offer" else "Edit offer").setView(form).setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val body = mapOf<String, Any>("name" to name.text.toString(), "description" to description.text.toString(), "price" to (price.text.toString().toDoubleOrNull() ?: 0.0), "offer_for" to (existing?.offer_for ?: "retailer"), "offer_type" to (existing?.offer_type ?: "bundle"), "start_date" to start.text.toString(), "end_date" to end.text.toString())
                if (existing == null) createOffer(body) else saveOffer(existing, body)
            }.show()
    }

    private fun createOffer(body: Map<String, Any>) = ApiClient.adminOffersApi.createOffer(body).enqueue(reloadCallback())

    private fun showOfferItemDialog(offer: AdminOffer) {
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 16, 48, 0) }
        val itemId = EditText(this).apply { hint = "Item ID" }
        val buyQty = EditText(this).apply { hint = "Buy quantity"; setText("1") }
        val freeQty = EditText(this).apply { hint = "Offer/free quantity"; setText("0") }
        val price = EditText(this).apply { hint = "Offer price"; setText("0") }
        listOf(itemId, buyQty, freeQty, price).forEach(form::addView)
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
