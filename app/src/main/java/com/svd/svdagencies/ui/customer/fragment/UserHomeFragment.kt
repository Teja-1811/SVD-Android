package com.svd.svdagencies.ui.customer.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.LinearLayout
import com.google.gson.JsonElement
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.svd.svdagencies.data.api.customer.ProductApi
import com.svd.svdagencies.data.model.customer.ProductResponse
import com.google.android.material.button.MaterialButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.customer.CustomerDashboardResponse
import com.svd.svdagencies.data.model.customer.CustomerOfferResponse
import com.svd.svdagencies.ui.customer.UserPortalActivity
import com.svd.svdagencies.ui.customer.CustomerSubscriptionsActivity
import com.svd.svdagencies.ui.customer.adapter.CustomerOffer
import com.svd.svdagencies.ui.customer.adapter.CustomerOfferAdapter
import com.svd.svdagencies.utils.NetworkMessageUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

/** Personal dashboard, independent of the retailer shop and bulk-order home. */
class UserHomeFragment : Fragment(R.layout.user_home) {
    private var productsCall: Call<JsonElement>? = null
    private var dashboardCall: Call<CustomerDashboardResponse>? = null
    private var offersCall: Call<CustomerOfferResponse>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.userBrowseProducts).setOnClickListener {
            (activity as? UserPortalActivity)?.openOrdersScreen()
        }
        view.findViewById<View>(R.id.userSubscriptions).setOnClickListener {
            startActivity(Intent(requireContext(), CustomerSubscriptionsActivity::class.java))
        }
        view.findViewById<SwipeRefreshLayout>(R.id.userRefresh).setOnRefreshListener { loadDashboard(); loadOffers(); loadProducts() }
    }

    override fun onResume() {
        super.onResume()
        loadDashboard()
        loadOffers()
        loadProducts()
    }

    private fun money(value: Double) = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(value)

    private fun loadDashboard() {
        val screen = view ?: return
        val refresh = screen.findViewById<SwipeRefreshLayout>(R.id.userRefresh)
        refresh.isRefreshing = true
        dashboardCall?.cancel()
        dashboardCall = ApiClient.customerApi.getDashboard()
        dashboardCall?.enqueue(object : Callback<CustomerDashboardResponse> {
            override fun onResponse(call: Call<CustomerDashboardResponse>, response: Response<CustomerDashboardResponse>) {
                if (view !== screen) return
                refresh.isRefreshing = false
                val dashboard = response.body()
                if (!response.isSuccessful || dashboard == null) {
                    showError("Unable to load your account. Pull down to retry.")
                    return
                }
                screen.findViewById<TextView>(R.id.userGreeting).text = "${when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
                    in 0..11 -> "Good morning"
                    in 12..16 -> "Good afternoon"
                    else -> "Good evening"
                }}, ${dashboard.customer.name}"
                screen.findViewById<TextView>(R.id.userAccountStatus).text = if (dashboard.customer.frozen) "Account needs attention" else dashboard.customer.accountStatus.orEmpty()
                screen.findViewById<TextView>(R.id.userWallet).text = money(dashboard.summary.walletBalance)
                screen.findViewById<TextView>(R.id.userOutstanding).text = money(dashboard.summary.outstandingDue)
                screen.findViewById<TextView>(R.id.userLatestOrder).text = dashboard.latestOrder?.let {
                    "${it.orderNumber} · ${it.status}\n${money(it.totalAmount + it.deliveryCharge)} · ${it.orderDate}"
                } ?: "Your orders will appear here after checkout."
            }

            override fun onFailure(call: Call<CustomerDashboardResponse>, error: Throwable) {
                if (call.isCanceled || view !== screen) return
                refresh.isRefreshing = false
                showError(NetworkMessageUtils.friendlyMessage(error, "Unable to load your account"))
            }
        })
    }

    private fun loadOffers() {
        val screen = view ?: return
        offersCall?.cancel()
        offersCall = ApiClient.customerApi.getOffers()
        offersCall?.enqueue(object : Callback<CustomerOfferResponse> {
            override fun onResponse(call: Call<CustomerOfferResponse>, response: Response<CustomerOfferResponse>) {
                if (view !== screen) return
                val offers = response.body()?.offers.orEmpty().map {
                    CustomerOffer(it.name, it.description.orEmpty().ifBlank { "View this offer" }, "Place offer order", "#0C4A6E", it.id)
                }
                val list = screen.findViewById<RecyclerView>(R.id.userOffers)
                list.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                list.adapter = CustomerOfferAdapter(offers) { selected ->
                    selected.offerId?.let { id ->
                        startActivity(Intent(requireContext(), com.svd.svdagencies.ui.customer.CustomerOfferOrderActivity::class.java).putExtra("offer_id", id))
                    }
                }
                screen.findViewById<View>(R.id.userOffersTitle).visibility = if (offers.isEmpty()) View.GONE else View.VISIBLE
                list.visibility = if (offers.isEmpty()) View.GONE else View.VISIBLE
            }
            override fun onFailure(call: Call<CustomerOfferResponse>, error: Throwable) = Unit
        })
    }

    private fun loadProducts() {
        val screen = view ?: return
        val list = screen.findViewById<LinearLayout>(R.id.userStockProducts)
        list.removeAllViews()
        list.addView(TextView(requireContext()).apply { text = "Loading available items…" })
        productsCall?.cancel()
        productsCall = ApiClient.retrofit.create(ProductApi::class.java).getProducts()
        productsCall?.enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (view !== screen) return
                list.removeAllViews()
                if (!response.isSuccessful) {
                    list.addView(TextView(requireContext()).apply { text = "Could not load items. Pull down to retry." })
                    return
                }
                val body = response.body()
                val rows = if (body?.isJsonObject == true) body.asJsonObject.get("products") else body
                val type = object : TypeToken<List<ProductResponse>>() {}.type
                val products = runCatching { Gson().fromJson<List<ProductResponse>>(rows, type) }.getOrNull().orEmpty().filter { it.stock > 0 }
                if (products.isEmpty()) list.addView(TextView(requireContext()).apply { text = "No items in stock. You can still place a dated pre-order in Orders." })
                products.forEach { product ->
                    list.addView(MaterialButton(requireContext()).apply {
                        text = "${product.name} · ${money(product.mrp)} · Order"
                        isAllCaps = false
                        setOnClickListener { (activity as? UserPortalActivity)?.openOrdersScreen(productId = product.id) }
                    })
                }
            }
            override fun onFailure(call: Call<JsonElement>, error: Throwable) {
                if (call.isCanceled || view !== screen) return
                list.removeAllViews()
                list.addView(TextView(requireContext()).apply { text = "Could not load items. Pull down to retry." })
            }
        })
    }

    private fun showError(message: String) {
        context?.let { Toast.makeText(it, message, Toast.LENGTH_LONG).show() }
    }

    override fun onDestroyView() {
        dashboardCall?.cancel()
        offersCall?.cancel()
        productsCall?.cancel()
        super.onDestroyView()
    }
}
