package com.svd.svdagencies.ui.customer.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.google.gson.JsonElement
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.svd.svdagencies.data.api.customer.ProductApi
import com.svd.svdagencies.data.model.customer.ProductResponse
import com.google.android.material.button.MaterialButton
import android.widget.Toast
import androidx.fragment.app.activityViewModels
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
    private val cart: com.svd.svdagencies.ui.customer.viewmodel.PlaceOrderViewModel by activityViewModels()
    private var stockAdapter: com.svd.svdagencies.ui.customer.adapter.UserStockProductAdapter? = null
    private var lastRefreshStarted = 0L
    private var productsCall: Call<JsonElement>? = null
    private var dashboardCall: Call<CustomerDashboardResponse>? = null
    private var offersCall: Call<CustomerOfferResponse>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        stockAdapter = com.svd.svdagencies.ui.customer.adapter.UserStockProductAdapter(cart.cartQuantities) { product ->
            val selected = cart.cartQuantities[product.id] ?: 0.0
            if (selected < product.stock) {
                cart.cartQuantities[product.id] = selected + 1.0
                cart.allProducts[product.id] = product.copy(selling_price = product.mrp, margin = 0.0)
                stockAdapter?.notifyDataSetChanged()
                updateCartButton()
            } else showError("All available units of this item are already in your cart.")
        }
        view.findViewById<RecyclerView>(R.id.userStockProducts).apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = stockAdapter
        }
        view.findViewById<View>(R.id.userViewCart).setOnClickListener {
            (activity as? UserPortalActivity)?.openOrdersScreen()
        }
        view.findViewById<View>(R.id.userOffersStatus).setOnClickListener { loadOffers() }
        updateCartButton()
        view.findViewById<View>(R.id.userBrowseProducts).setOnClickListener {
            (activity as? UserPortalActivity)?.openOrdersScreen()
        }
        view.findViewById<View>(R.id.userSubscriptions).setOnClickListener {
            startActivity(Intent(requireContext(), CustomerSubscriptionsActivity::class.java))
        }
        view.findViewById<SwipeRefreshLayout>(R.id.userRefresh).setOnRefreshListener { refreshHome(force = true) }
    }

    override fun onResume() {
        super.onResume()
        refreshHome()
        updateCartButton()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && view != null) {
            stockAdapter?.notifyDataSetChanged()
            refreshHome()
            updateCartButton()
        }
    }

    private fun refreshHome(force: Boolean = false) {
        if (view == null || isHidden) return
        val now = android.os.SystemClock.elapsedRealtime()
        // show() and onResume can arrive together; do not cancel and restart the same requests.
        if (!force && lastRefreshStarted != 0L && now - lastRefreshStarted < 1500L) return
        lastRefreshStarted = now
        loadDashboard()
        loadOffers()
        loadProducts()
    }

    private fun updateCartButton() {
        val button = view?.findViewById<MaterialButton>(R.id.userViewCart) ?: return
        val count = cart.cartQuantities.values.sum().toInt()
        button.visibility = if (count > 0) View.VISIBLE else View.GONE
        button.text = "View cart ($count)"
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
                screen.findViewById<View>(R.id.userOutstandingLabel).visibility = if (dashboard.customer.allowCredit) View.VISIBLE else View.GONE
                screen.findViewById<View>(R.id.userOutstanding).visibility = if (dashboard.customer.allowCredit) View.VISIBLE else View.GONE
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
                val offerStatus = screen.findViewById<TextView>(R.id.userOffersStatus)
                if (!response.isSuccessful || response.body() == null) {
                    offerStatus.text = "Could not load offers. Tap to retry."
                    offerStatus.visibility = View.VISIBLE
                    return
                }
                val offers = response.body()?.offers.orEmpty().map {
                    CustomerOffer(it.name, it.description.orEmpty().ifBlank { money(it.price) }, "Place offer order", "#0C4A6E", it.id)
                }
                val list = screen.findViewById<RecyclerView>(R.id.userOffers)
                list.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                list.adapter = CustomerOfferAdapter(offers, R.layout.user_offer_card) { selected ->
                    selected.offerId?.let { id ->
                        startActivity(Intent(requireContext(), com.svd.svdagencies.ui.customer.CustomerOfferOrderActivity::class.java).putExtra("offer_id", id))
                    }
                }
                screen.findViewById<View>(R.id.userOffersTitle).visibility = View.VISIBLE
                offerStatus.text = "No active offers right now."
                offerStatus.visibility = if (offers.isEmpty()) View.VISIBLE else View.GONE
                list.visibility = if (offers.isEmpty()) View.GONE else View.VISIBLE
            }
            override fun onFailure(call: Call<CustomerOfferResponse>, error: Throwable) {
                if (call.isCanceled || view !== screen) return
                screen.findViewById<TextView>(R.id.userOffersStatus).apply {
                    text = "Could not load offers. Tap to retry."
                    visibility = View.VISIBLE
                }
            }
        })
    }

    private fun loadProducts() {
        val screen = view ?: return
        val list = screen.findViewById<RecyclerView>(R.id.userStockProducts)
        val status = screen.findViewById<TextView>(R.id.userStockProductsStatus)
        status.text = "Loading available items..."
        status.visibility = View.VISIBLE
        list.visibility = View.GONE
        productsCall?.cancel()
        productsCall = ApiClient.retrofit.create(ProductApi::class.java).getProducts()
        productsCall?.enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (view !== screen) return
                if (!response.isSuccessful) {
                    status.text = "Could not load items. Pull down to retry."
                    return
                }
                val body = response.body()
                val rows = if (body?.isJsonObject == true) body.asJsonObject.get("products") else body
                val type = object : TypeToken<List<ProductResponse>>() {}.type
                val products = runCatching { Gson().fromJson<List<ProductResponse>>(rows, type) }.getOrNull().orEmpty().filter { it.stock > 0 }
                stockAdapter?.submitProducts(products)
                list.visibility = if (products.isEmpty()) View.GONE else View.VISIBLE
                status.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
                status.text = "No items in stock. You can still place a dated pre-order in Orders."
                updateCartButton()
            }
            override fun onFailure(call: Call<JsonElement>, error: Throwable) {
                if (call.isCanceled || view !== screen) return
                status.text = "Could not load items. Pull down to retry."
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
        view?.findViewById<RecyclerView>(R.id.userStockProducts)?.adapter = null
        stockAdapter = null
        lastRefreshStarted = 0L
        super.onDestroyView()
    }
}
