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
import com.svd.svdagencies.ui.customer.CustomerContactSupportActivity
import com.svd.svdagencies.ui.customer.CustomerMainActivity
import com.svd.svdagencies.ui.customer.CustomerRaisedQueriesActivity
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
    private lateinit var tvStatusPill: TextView
    private lateinit var tvStatusMessage: TextView
    private lateinit var tvOrderWindow: TextView
    private lateinit var tvLatestOrderTitle: TextView
    private lateinit var tvLatestOrderSubtitle: TextView
    private lateinit var tvLatestOrderItems: TextView
    private lateinit var btnLatestOrderAction: Button
    private lateinit var actionPlaceOrder: LinearLayout
    private lateinit var actionBills: LinearLayout
    private lateinit var actionPayment: LinearLayout
    private lateinit var actionSupport: LinearLayout

    private lateinit var api: CustomerApi
    private lateinit var session: SessionManager

    private val orderDateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvWelcome = view.findViewById(R.id.tvWelcome)
        tvPhone = view.findViewById(R.id.tvPhone)
        tvBalance = view.findViewById(R.id.tvBalance)
        tvShop = view.findViewById(R.id.tvShop)
        tvStatus = view.findViewById(R.id.tvStatus)
        tvStatusPill = view.findViewById(R.id.tvStatusPill)
        tvStatusMessage = view.findViewById(R.id.tvStatusMessage)
        tvOrderWindow = view.findViewById(R.id.tvOrderWindow)
        tvLatestOrderTitle = view.findViewById(R.id.tvLatestOrderTitle)
        tvLatestOrderSubtitle = view.findViewById(R.id.tvLatestOrderSubtitle)
        tvLatestOrderItems = view.findViewById(R.id.tvLatestOrderItems)
        btnLatestOrderAction = view.findViewById(R.id.btnLatestOrderAction)
        actionPlaceOrder = view.findViewById(R.id.actionPlaceOrder)
        actionBills = view.findViewById(R.id.actionBills)
        actionPayment = view.findViewById(R.id.actionPayment)
        actionSupport = view.findViewById(R.id.actionSupport)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        session = SessionManager(requireContext())

        if (session.getUserId() == -1) {
            Toast.makeText(requireContext(), "Session expired", Toast.LENGTH_SHORT).show()
            return
        }

        api = ApiClient.retrofit.create(CustomerApi::class.java)
        setupQuickActions()

        RefreshManager.setupRefresh(swipeRefresh) {
            loadDashboard()
        }

        renderLatestOrderCard()
        RefreshManager.startRefresh(swipeRefresh)
        loadDashboard()
    }

    override fun onResume() {
        super.onResume()
        renderLatestOrderCard()
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
            tvStatusPill.text = "Synced"
            tvStatusMessage.text = "Account is active and ready for delivery updates"
            tvStatusMessage.setTextColor(ContextCompat.getColor(context, R.color.icon_green))
            tvStatusPill.backgroundTintList = ContextCompat.getColorStateList(context, R.color.customer_positive_bg)
            tvStatusPill.setTextColor(ContextCompat.getColor(context, R.color.icon_green))
        } else {
            tvStatus.text = "Attention Needed"
            tvStatusPill.text = "Check Account"
            tvStatusMessage.text = "Account needs attention before the next delivery cycle"
            tvStatusMessage.setTextColor(ContextCompat.getColor(context, R.color.customer_badge_text))
            tvStatusPill.backgroundTintList = ContextCompat.getColorStateList(context, R.color.customer_warning_bg)
            tvStatusPill.setTextColor(ContextCompat.getColor(context, R.color.customer_badge_text))
        }
    }

    private fun renderLatestOrderCard() {
        val context = context ?: return
        val latestOrder = LatestCustomerOrderStore.get(context)
        val isOrderingOpen = CustomerOrderWindow.isOpen()

        tvOrderWindow.text = CustomerOrderWindow.statusMessage()

        if (latestOrder == null) {
            tvLatestOrderTitle.text = "Ready for today's order?"
            tvLatestOrderSubtitle.text = "Place a fresh order between 9:00 AM and 8:00 PM."
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

    private fun setupQuickActions() {
        actionPlaceOrder.setOnClickListener {
            (activity as? CustomerMainActivity)?.openOrdersScreen(editLatestOrder = false)
        }
        actionBills.setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.customerBottomNav)?.selectedItemId = R.id.nav_bills
        }
        actionPayment.setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.customerBottomNav)?.selectedItemId = R.id.nav_payment
        }
        actionSupport.setOnClickListener {
            startActivity(Intent(requireContext(), CustomerContactSupportActivity::class.java))
        }
        tvStatusPill.setOnClickListener {
            startActivity(Intent(requireContext(), CustomerRaisedQueriesActivity::class.java))
        }
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
