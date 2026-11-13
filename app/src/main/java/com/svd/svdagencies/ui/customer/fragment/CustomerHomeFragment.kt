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
import com.svd.svdagencies.data.api.customer.OrderApi
import com.svd.svdagencies.data.model.customer.CustomerDashboardResponse
import com.svd.svdagencies.data.model.customer.CustomerOfferResponse
import com.svd.svdagencies.data.model.customer.OfferItem
import com.svd.svdagencies.ui.customer.CustomerContactSupportActivity
import com.svd.svdagencies.ui.customer.CustomerHistoryActivity
import com.svd.svdagencies.ui.customer.PortalMainActivity
import com.svd.svdagencies.ui.customer.CustomerRaisedQueriesActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.ui.customer.adapter.CustomerOffer
import com.svd.svdagencies.ui.customer.adapter.CustomerOfferAdapter
import com.svd.svdagencies.utils.RefreshManager
import com.svd.svdagencies.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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
    private lateinit var btnLatestOrderCancel: Button
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

    private var dashboardSnapshot: CustomerDashboardResponse? = null

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
        btnLatestOrderCancel = view.findViewById(R.id.btnLatestOrderCancel)
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


                if (!isAdded || view == null) return

                val dashboard = response.body() ?: return
                val customer = dashboard.customer
                val summary = dashboard.summary
                tvWelcome.text = "Welcome back, ${customer.name}"
                tvPhone.text = "Mobile: ${customer.phone ?: "Not available"}"
                tvShop.text = customer.shopName ?: "Shop not set"
                val hasOutstanding = summary.outstandingDue > 0.0
                view?.findViewById<TextView>(R.id.retailerAccountTitle)?.text =
                    if (hasOutstanding) "Outstanding" else "Wallet"
                tvBalance.text = formatCurrency(
                    if (hasOutstanding) summary.outstandingDue else summary.walletBalance
                )
                // The account card is deliberately a single state: due or
                // wallet advance.  Never show both balances together.
                view?.findViewById<TextView>(R.id.retailerOutstanding)?.visibility = View.GONE
                dashboardSnapshot = dashboard
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
            tvStatusMessage.text = "Account needs attention."
            tvStatusMessage.setTextColor(ContextCompat.getColor(context, R.color.brand_red))
        }
    }

    private fun setupOffersCarousel() {
        rvOffers.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvOffers.adapter = CustomerOfferAdapter(offersList) { selected ->
            selected.offerId?.let { id ->
                startActivity(android.content.Intent(requireContext(), com.svd.svdagencies.ui.customer.CustomerOfferOrderActivity::class.java).putExtra("offer_id", id))
            }
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
                        val bundlePrice = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("en-IN")).format(item.price)
                        val subtitle = "$bundlePrice for the bundle · ${item.items.size} products"


                        offersList.add(
                            CustomerOffer(
                                title = item.name,
                                subtitle = subtitle,
                                actionText = "View bundle",
                                colorHex = colors[index % colors.size],
                                offerId = item.id
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
        val dashboard = dashboardSnapshot
        val latestBill = dashboard?.latestBill
        val tomorrowOrder = dashboard?.latestOrder

        // Give a scheduled delivery the highest priority.  If there is no
        // order due tomorrow, fall back to the latest billed invoice.
        // Do not show the generic order-window copy here; it is not an order.
        when {
            tomorrowOrder != null -> {
                tvOrderWindow.isVisible = true
                tvOrderWindow.text = "DELIVERY TOMORROW"
                tvLatestOrderTitle.text = tomorrowOrder.orderNumber
                tvLatestOrderSubtitle.text = "Delivery on ${formatDashboardDate(tomorrowOrder.deliveryDate ?: tomorrowOrder.orderDate)}"
                val payment = tomorrowOrder.paymentStatus
                    ?.uppercase(Locale.getDefault())
                    ?.let { " · Payment: $it" }
                    .orEmpty()
                val orderAmount = (tomorrowOrder.approvedTotalAmount.takeIf { it > 0.0 }
                    ?: tomorrowOrder.totalAmount) + tomorrowOrder.deliveryCharge
                tvLatestOrderItems.text = "Order total: ${formatCurrency(orderAmount)}$payment"
                tvLatestOrderItems.isVisible = true
            }
            latestBill != null -> {
                tvOrderWindow.isVisible = true
                tvOrderWindow.text = "LAST BILLED"
                tvLatestOrderTitle.text = latestBill.invoiceNumber
                tvLatestOrderSubtitle.text = "Billed on ${formatDashboardDate(latestBill.invoiceDate)}"
                tvLatestOrderItems.text = "Invoice total: ${formatCurrency(latestBill.totalAmount)}"
                tvLatestOrderItems.isVisible = true
            }
            else -> {
                tvOrderWindow.isVisible = false
                tvLatestOrderTitle.text = "No recent bill or scheduled delivery"
                tvLatestOrderSubtitle.text = "Your latest billed invoice or tomorrow's order will appear here."
                tvLatestOrderItems.isVisible = false
            }
        }

        val canUpdateTomorrowOrder = tomorrowOrder?.canUpdate == true
        val canCancelTomorrowOrder = tomorrowOrder?.canCancel == true
        btnLatestOrderAction.text = if (canUpdateTomorrowOrder) "Update Order" else "View Orders"
        btnLatestOrderAction.isEnabled = true
        btnLatestOrderAction.setOnClickListener {
            if (canUpdateTomorrowOrder) {
                (activity as? PortalMainActivity)?.openOrdersScreen(editLatestOrder = true)
            } else {
                startActivity(Intent(requireContext(), CustomerHistoryActivity::class.java))
            }
        }
        btnLatestOrderCancel.isVisible = canCancelTomorrowOrder
        btnLatestOrderCancel.setOnClickListener {
            if (tomorrowOrder != null && canCancelTomorrowOrder) confirmCancelOrder(tomorrowOrder.orderId)
        }
    }

    private fun confirmCancelOrder(orderId: Int) {
        com.svd.svdagencies.utils.WhiteMaterialAlertDialogBuilder(requireContext())
            .setTitle("Cancel order?")
            .setMessage("This order has not been confirmed. It will be cancelled and kept in your order history.")
            .setNegativeButton("Keep order", null)
            .setPositiveButton("Cancel order") { _, _ ->
                btnLatestOrderCancel.isEnabled = false
                ApiClient.retrofit.create(OrderApi::class.java).cancelOrder(orderId)
                    .enqueue(object : Callback<Map<String, Any>> {
                        override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                            if (!isAdded) return
                            btnLatestOrderCancel.isEnabled = true
                            Toast.makeText(requireContext(), if (response.isSuccessful) "Order cancelled" else "Order can no longer be cancelled", Toast.LENGTH_SHORT).show()
                            if (response.isSuccessful) loadDashboard()
                        }

                        override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                            if (!isAdded) return
                            btnLatestOrderCancel.isEnabled = true
                            Toast.makeText(requireContext(), "Unable to cancel order", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .show()
    }

    private fun formatCurrency(amount: Double): String = "\u20B9 " + String.format(Locale.getDefault(), "%.2f", amount)

    private fun formatDashboardDate(value: String): String {
        return value.split("-").let { parts ->
            if (parts.size != 3) return value
            val month = parts[1].toIntOrNull() ?: return value
            val day = parts[2].toIntOrNull() ?: return value
            val monthName = java.text.DateFormatSymbols(Locale.getDefault()).shortMonths.getOrNull(month - 1)
                ?: return value
            "$day $monthName ${parts[0]}"
        }
    }
}
