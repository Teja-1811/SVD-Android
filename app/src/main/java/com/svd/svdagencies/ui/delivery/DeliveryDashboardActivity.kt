package com.svd.svdagencies.ui.delivery

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.google.android.material.tabs.TabLayout
import com.svd.svdagencies.R
import com.svd.svdagencies.base.BaseActivity
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.delivery.DeliveryItem
import com.svd.svdagencies.data.model.delivery.DeliveryTodayResponse
import com.svd.svdagencies.data.model.delivery.DeliveryUpdateRequest
import com.svd.svdagencies.data.model.delivery.DeliveryUpdateResponse
import com.svd.svdagencies.databinding.DeliveryDashboardBinding
import com.svd.svdagencies.ui.auth.LoginActivity
import com.svd.svdagencies.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeliveryDashboardActivity : BaseActivity() {

    private lateinit var binding: DeliveryDashboardBinding
    private lateinit var adapter: DeliveryAdapter
    private lateinit var sessionManager: SessionManager
    private var pendingList = emptyList<DeliveryItem>()
    private var completedList = emptyList<DeliveryItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DeliveryDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sessionManager = SessionManager(this)

        setupToolbar(binding.toolbar, "Delivery Hub")
        setupHeader()
        setupRecyclerView()
        setupTabs()
        setupSwipeRefresh()
        setupGenerateBill()
        loadDeliveries()
    }

    private fun setupToolbar(toolbar: Toolbar, title: String) {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.findViewById<TextView>(R.id.tvToolbarTitle)?.text = title
        toolbar.findViewById<ImageButton>(R.id.btnLogout)?.setOnClickListener {
            handleLogout()
        }
    }

    private fun setupHeader() {
        binding.tvTodayLabel.text = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date())
        binding.tvHeadline.text = "Keep every stop on track"
        binding.tvSubHeadline.text = "Orders and subscription drops are grouped in one route list."
        renderSummary()
    }

    private fun setupRecyclerView() {
        adapter = DeliveryAdapter({ item, newStatus ->
            updateStatus(item, newStatus)
        }, { item ->
            val intent = android.content.Intent(this, DeliveryCreateBillActivity::class.java).apply {
                putExtra("customer_id", item.customerId)
                putExtra("customer_name", item.customerName)
            }
            startActivity(intent)
        })
        binding.rvDeliveries.adapter = adapter
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                updateListForSelectedTab()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit

            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })
        updateTabTitles()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener { loadDeliveries() }
    }

    private fun setupGenerateBill() {
        binding.btnGenerateBill.setOnClickListener {
            startActivity(android.content.Intent(this, DeliveryCreateBillActivity::class.java))
        }
    }

    private fun loadDeliveries() {
        binding.swipeRefresh.isRefreshing = true
        ApiClient.deliveryApi.getTodayDeliveries().enqueue(object : Callback<DeliveryTodayResponse> {
            override fun onResponse(
                call: Call<DeliveryTodayResponse>,
                response: Response<DeliveryTodayResponse>
            ) {
                binding.swipeRefresh.isRefreshing = false
                if (response.isSuccessful) {
                    val data = response.body()
                    pendingList = data?.pending.orEmpty().sortedBy { it.status.sortPriority() }
                    completedList = data?.completed.orEmpty()
                    renderSummary()
                    updateTabTitles()
                    updateListForSelectedTab()
                } else {
                    Toast.makeText(
                        this@DeliveryDashboardActivity,
                        "Failed to load deliveries",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<DeliveryTodayResponse>, t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(
                    this@DeliveryDashboardActivity,
                    "Error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun renderSummary() {
        val total = pendingList.size + completedList.size
        binding.tvPendingCount.text = pendingList.size.toString()
        binding.tvCompletedCount.text = completedList.size.toString()
        binding.tvTotalStops.text = total.toString()
    }

    private fun updateTabTitles() {
        binding.tabLayout.getTabAt(0)?.text = "Pending (${pendingList.size})"
        binding.tabLayout.getTabAt(1)?.text = "Completed (${completedList.size})"
    }

    private fun updateListForSelectedTab() {
        val showingPending = binding.tabLayout.selectedTabPosition == 0
        val currentList = if (showingPending) pendingList else completedList

        adapter.submitList(currentList)
        binding.rvDeliveries.visibility = if (currentList.isEmpty()) View.GONE else View.VISIBLE
        binding.layoutEmpty.visibility = if (currentList.isEmpty()) View.VISIBLE else View.GONE
        binding.tvEmptyTitle.text = if (showingPending) "All clear" else "Nothing delivered yet"
        binding.tvEmptyMessage.text = if (showingPending) {
            "No pending stops for today."
        } else {
            "Completed deliveries will appear here once they are marked delivered."
        }
    }

    private fun updateStatus(item: DeliveryItem, newStatus: String) {
        val request = DeliveryUpdateRequest(
            type = item.type,
            deliveryId = item.id,
            status = newStatus,
            deliveredAmount = if (newStatus == "delivered" && item.type == "order") item.totalAmount else null,
            deliveredAt = if (newStatus == "delivered") currentIsoTimestamp() else null
        )

        showScreenLoading()
        ApiClient.deliveryApi.updateDelivery(request).enqueue(object : Callback<DeliveryUpdateResponse> {
            override fun onResponse(
                call: Call<DeliveryUpdateResponse>,
                response: Response<DeliveryUpdateResponse>
            ) {
                hideScreenLoading()
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(
                        this@DeliveryDashboardActivity,
                        "Delivery updated",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadDeliveries()
                } else {
                    Toast.makeText(
                        this@DeliveryDashboardActivity,
                        response.body()?.message ?: "Failed to update delivery",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<DeliveryUpdateResponse>, t: Throwable) {
                hideScreenLoading()
                Toast.makeText(
                    this@DeliveryDashboardActivity,
                    "Error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun currentIsoTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).format(Date())
    }

    private fun handleLogout() {
        sessionManager.logout()
        startActivity(
            android.content.Intent(this, LoginActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}

private fun String.sortPriority(): Int {
    return when (lowercase(Locale.getDefault())) {
        "out_for_delivery" -> 0
        "pending" -> 1
        "delivered" -> 2
        else -> 3
    }
}
