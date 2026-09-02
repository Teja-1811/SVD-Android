package com.svd.svdagencies.ui.customer.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.api.customer.CustomerApi
import com.svd.svdagencies.data.model.customer.CustomerDashboardResponse
import com.svd.svdagencies.data.model.customer.CustomerOfferResponse
import com.svd.svdagencies.data.model.customer.OfferItem
import com.svd.svdagencies.ui.customer.CustomerContactSupportActivity
import com.svd.svdagencies.ui.customer.CustomerMainActivity
import com.svd.svdagencies.ui.customer.CustomerRaisedQueriesActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.ui.customer.adapter.CustomerOffer
import com.svd.svdagencies.ui.customer.adapter.CustomerOfferAdapter
import com.svd.svdagencies.utils.CustomerOrderWindow
import com.svd.svdagencies.utils.LatestCustomerOrder
import com.svd.svdagencies.utils.LatestCustomerOrderStore
import com.svd.svdagencies.utils.RefreshManager
import com.svd.svdagencies.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CustomerHomeFragment : Fragment(R.layout.customer_home) {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvWelcome: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvBalance: TextView
    private lateinit var tvShop: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvStatusMessage: TextView
    private lateinit var tvOrderWindow: TextView
    private lateinit var tvLatestOrderTitle: TextView
    private lateinit var tvLatestOrderSubtitle: TextView
    private lateinit var tvLatestOrderItems: TextView
    private lateinit var btnLatestOrderAction: Button
    private lateinit var rvOffers: RecyclerView

    private val slidingHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val slidingRunnable = object : Runnable {
        override fun run() {
            val count = rvOffers.adapter?.itemCount ?: 0
            if (count > 0) {
                val layoutManager = rvOffers.layoutManager as? LinearLayoutManager
                val current = layoutManager?.findFirstVisibleItemPosition() ?: 0
                val next = (current + 1) % count
                rvOffers.smoothScrollToPosition(next)
            }
            slidingHandler.postDelayed(this, 4000) // Slide every 4 seconds
        }
    }

    private lateinit var api: CustomerApi
    private lateinit var session: SessionManager
    private val offersList = mutableListOf<CustomerOffer>()

    private val orderDateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        tvWelcome = view.findViewById(R.id.tvWelcome)
        tvPhone = view.findViewById(R.id.tvPhone)
        tvBalance = view.findViewById(R.id.tvBalance)
        tvShop = view.findViewById(R.id.tvShop)
        tvStatus = view.findViewById(R.id.tvStatus)
        tvStatusMessage = view.findViewById(R.id.tvStatusMessage)
        tvOrderWindow = view.findViewById(R.id.tvOrderWindow)
        tvLatestOrderTitle = view.findViewById(R.id.tvLatestOrderTitle)
        tvLatestOrderSubtitle = view.findViewById(R.id.tvLatestOrderSubtitle)
        tvLatestOrderItems = view.findViewById(R.id.tvLatestOrderItems)
        btnLatestOrderAction = view.findViewById(R.id.btnLatestOrderAction)
        rvOffers = view.findViewById(R.id.rvOffers)

        session = SessionManager(requireContext())
        api = ApiClient.retrofit.create(CustomerApi::class.java)
        
        setupOffersCarousel()

        RefreshManager.setupRefresh(swipeRefresh) {
            loadDashboard()
            loadOffers()
        }

        renderLatestOrderCard()
    }

    override fun onResume() {
        super.onResume()
        RefreshManager.startRefresh(swipeRefresh)
        loadDashboard()
        loadOffers()
        renderLatestOrderCard()
        startAutoSliding()
    }

    override fun onPause() {
        super.onPause()
        stopAutoSliding()
    }

    private fun startAutoSliding() {
        stopAutoSliding()
        slidingHandler.postDelayed(slidingRunnable, 4000)
    }

    private fun stopAutoSliding() {
        slidingHandler.removeCallbacks(slidingRunnable)
    }

    private fun loadDashboard() {
        api.getDashboard().enqueue(object : Callback<CustomerDashboardResponse> {
            override fun onResponse(
                call: Call<CustomerDashboardResponse>,
                response: Response<CustomerDashboardResponse>
            ) {
                val context = context ?: return

                RefreshManager.stopRefresh(swipeRefresh)

                if (!response.isSuccessful) {
                    Toast.makeText(context, "Server error: ${response.code()}", Toast.LENGTH_LONG).show()
                    return
                }

                Log.d("CustomerHomeFragment", "Response: ${response.body()}")

                if (!isAdded || view == null) return

                val dashboard = response.body() ?: return
                val customer = dashboard.customer
                val summary = dashboard.summary
                tvWelcome.text = "Welcome back, ${customer.name}"
                tvPhone.text = "Mobile: ${customer.phone ?: "Not available"}"
                tvShop.text = customer.shopName ?: "Shop not set"
                tvBalance.text = formatCurrency(summary.balance)
                applyAccountState(dashboard)
                renderLatestOrderCard()
            }

            override fun onFailure(call: Call<CustomerDashboardResponse>, t: Throwable) {
                val context = context ?: return
                RefreshManager.stopRefresh(swipeRefresh)
                Toast.makeText(context, "Network error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun applyAccountState(dashboard: CustomerDashboardResponse) {
        val context = context ?: return
        val isActive = dashboard.customer.accountStatus.equals("Active", ignoreCase = true) && !dashboard.customer.frozen

        if (isActive) {
            tvStatus.text = "Active"
            tvStatusMessage.text = "Your account is synced and ready."
            tvStatusMessage.setTextColor(ContextCompat.getColor(context, R.color.icon_green))
        } else {
            tvStatus.text = "Attention Needed"
            tvStatusMessage.text = "Account needs attention before the next delivery cycle"
            tvStatusMessage.setTextColor(ContextCompat.getColor(context, R.color.brand_red))
        }
    }

    private fun setupOffersCarousel() {
        rvOffers.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvOffers.adapter = CustomerOfferAdapter(offersList) {
            // Action for the offer - navigate to orders
            (activity as? CustomerMainActivity)?.openOrdersScreen()
        }
        
        if (rvOffers.onFlingListener == null) {
            PagerSnapHelper().attachToRecyclerView(rvOffers)
        }
    }

    private fun loadOffers() {
        api.getOffers().enqueue(object : Callback<CustomerOfferResponse> {
            override fun onResponse(call: Call<CustomerOfferResponse>, response: Response<CustomerOfferResponse>) {
                if (!isAdded || view == null) return
                
                if (response.isSuccessful && response.body()?.status == true) {
                    val remoteOffers = response.body()?.offers ?: emptyList()
                    offersList.clear()
                    
                    val colors = listOf("#0C4A6E", "#D32F2F", "#35BFA0", "#7B1FA2", "#F57C00")
                    
                    remoteOffers.forEachIndexed { index, item ->
                        var subtitle = item.description ?: ""
                        if (subtitle.isBlank() && item.offerType == "buy_x_get_y" && item.items.isNotEmpty()) {
                            val first = item.items[0]
                            subtitle = "Buy ${first.buyQty} get ${first.offerQty} ${first.itemName} free!"
                        }

                        offersList.add(
                            CustomerOffer(
                                title = item.name,
                                subtitle = subtitle.ifBlank { "Special discount available" },
                                actionText = "Order Now",
                                colorHex = colors[index % colors.size]
                            )
                        )
                    }
                    
                    rvOffers.adapter?.notifyDataSetChanged()
                    rvOffers.isVisible = offersList.isNotEmpty()
                }
            }

            override fun onFailure(call: Call<CustomerOfferResponse>, t: Throwable) {
                // Silently fail for offers as it's secondary to dashboard
            }
        })
    }

    private fun renderLatestOrderCard() {
        val context = context ?: return
        val latestOrder = LatestCustomerOrderStore.get(context)
        val isOrderingOpen = CustomerOrderWindow.isOpen()

        tvOrderWindow.text = CustomerOrderWindow.statusMessage()

        if (latestOrder == null) {
            tvLatestOrderTitle.text = "Ready for today's order?"
            tvLatestOrderSubtitle.text = "Place a fresh order between 9:00 AM and 4:00 PM."
            tvLatestOrderItems.text = "Your latest order will appear here after checkout."
            btnLatestOrderAction.text = "Place Order"
            btnLatestOrderAction.isEnabled = isOrderingOpen
            btnLatestOrderAction.setOnClickListener {
                (activity as? CustomerMainActivity)?.openOrdersScreen(editLatestOrder = false)
            }
            return
        }

        bindLatestOrder(latestOrder, isOrderingOpen)
    }

    private fun bindLatestOrder(latestOrder: LatestCustomerOrder, isOrderingOpen: Boolean) {
        val totalUnits = latestOrder.items.sumOf { it.quantity }
        val placedAt = orderDateFormat.format(Date(latestOrder.placedAtMillis))
        val itemLines = latestOrder.items.joinToString(separator = "\n") { item -> "- ${item.name} (${formatQuantity(item.quantity)})" }

        tvLatestOrderTitle.text = latestOrder.orderNumber?.let { "Latest Order: $it" } ?: "Latest Order"
        tvLatestOrderSubtitle.text = "${formatQuantity(totalUnits)} units - Placed on $placedAt"
        tvLatestOrderItems.text = itemLines
        tvLatestOrderItems.isVisible = itemLines.isNotBlank()
        btnLatestOrderAction.text = if (isOrderingOpen) "Edit Latest Order" else "Order Window Closed"
        btnLatestOrderAction.isEnabled = isOrderingOpen
        btnLatestOrderAction.setOnClickListener {
            (activity as? CustomerMainActivity)?.openOrdersScreen(editLatestOrder = true)
        }
    }

    private fun formatCurrency(amount: Double): String = "\u20B9 " + String.format(Locale.getDefault(), "%.2f", amount)

    private fun formatQuantity(quantity: Double): String {
        return if (quantity == quantity.toInt().toDouble()) quantity.toInt().toString() else quantity.toString()
    }
}
