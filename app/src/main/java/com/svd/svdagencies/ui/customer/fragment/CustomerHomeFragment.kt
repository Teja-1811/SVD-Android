package com.svd.svdagencies.ui.customer.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.api.customer.CustomerApi
import com.svd.svdagencies.data.model.customer.CustomerDashboardResponse
import com.svd.svdagencies.ui.customer.CustomerMainActivity
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
    private lateinit var tvBalance: TextView
    private lateinit var tvShop: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvOrderWindow: TextView
    private lateinit var tvLatestOrderTitle: TextView
    private lateinit var tvLatestOrderSubtitle: TextView
    private lateinit var tvLatestOrderItems: TextView
    private lateinit var btnLatestOrderAction: Button

    private lateinit var api: CustomerApi
    private lateinit var session: SessionManager
    private var userId: Int = -1

    private val orderDateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvWelcome = view.findViewById(R.id.tvWelcome)
        tvBalance = view.findViewById(R.id.tvBalance)
        tvShop = view.findViewById(R.id.tvShop)
        tvStatus = view.findViewById(R.id.tvStatus)
        tvOrderWindow = view.findViewById(R.id.tvOrderWindow)
        tvLatestOrderTitle = view.findViewById(R.id.tvLatestOrderTitle)
        tvLatestOrderSubtitle = view.findViewById(R.id.tvLatestOrderSubtitle)
        tvLatestOrderItems = view.findViewById(R.id.tvLatestOrderItems)
        btnLatestOrderAction = view.findViewById(R.id.btnLatestOrderAction)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        session = SessionManager(requireContext())
        userId = session.getUserId()

        if (userId == -1) {
            Toast.makeText(requireContext(), "Session expired", Toast.LENGTH_SHORT).show()
            return
        }

        api = ApiClient.retrofit.create(CustomerApi::class.java)

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
        api.getDashboard(userId).enqueue(object : Callback<CustomerDashboardResponse> {
            override fun onResponse(
                call: Call<CustomerDashboardResponse>,
                response: Response<CustomerDashboardResponse>
            ) {
                val context = context ?: return

                RefreshManager.stopRefresh(swipeRefresh)

                if (!response.isSuccessful) {
                    Toast.makeText(
                        context,
                        "Server error: ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                Log.d("CustomerHomeFragment", "Response: ${response.body()}")

                val customer = response.body() ?: return
                tvWelcome.text = "Welcome back, ${customer.customerName}"
                tvShop.text = customer.shopName
                tvBalance.text = "\u20B9 ${customer.balance}"
                renderLatestOrderCard()

                if (customer.accountStatus == "Active") {
                    tvStatus.text = "Active"
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.icon_green))
                } else {
                    tvStatus.text = "Inactive"
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.login_text_red))
                }
            }

            override fun onFailure(call: Call<CustomerDashboardResponse>, t: Throwable) {
                val context = context ?: return

                RefreshManager.stopRefresh(swipeRefresh)
                Toast.makeText(
                    context,
                    "Network error: ${t.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
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

    private fun bindLatestOrder(latestOrder: LatestCustomerOrder, isOrderingOpen: Boolean) {
        val totalUnits = latestOrder.items.sumOf { it.quantity }
        val placedAt = orderDateFormat.format(Date(latestOrder.placedAtMillis))
        val itemLines = latestOrder.items.joinToString(separator = "\n") { item ->
            "- ${item.name} (${formatQuantity(item.quantity)})"
        }

        tvLatestOrderTitle.text =
            latestOrder.orderNumber?.let { "Latest Order: $it" } ?: "Latest Order"
        tvLatestOrderSubtitle.text =
            "${formatQuantity(totalUnits)} units - Placed on $placedAt"
        tvLatestOrderItems.text = itemLines
        tvLatestOrderItems.isVisible = itemLines.isNotBlank()
        btnLatestOrderAction.text = if (isOrderingOpen) "Edit Latest Order" else "Order Window Closed"
        btnLatestOrderAction.isEnabled = isOrderingOpen
        btnLatestOrderAction.setOnClickListener {
            (activity as? CustomerMainActivity)?.openOrdersScreen(editLatestOrder = true)
        }
    }

    private fun formatQuantity(quantity: Double): String {
        return if (quantity == quantity.toInt().toDouble()) {
            quantity.toInt().toString()
        } else {
            quantity.toString()
        }
    }
}
