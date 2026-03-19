package com.svd.svdagencies.ui.admin

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.AdminDeliveryDashboardResponse
import com.svd.svdagencies.data.model.admin.AdminDeliveryEntry
import com.svd.svdagencies.data.model.delivery.DeliveryUpdateRequest
import com.svd.svdagencies.data.model.delivery.DeliveryUpdateResponse
import com.svd.svdagencies.databinding.AdminUserDeliveryBinding
import com.svd.svdagencies.ui.admin.adapter.AdminDeliveryDashboardAdapter
import com.svd.svdagencies.utils.NetworkMessageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdminUserDeliveryActivity : AdminBaseActivity() {

    private lateinit var binding: AdminUserDeliveryBinding
    private lateinit var adapter: AdminDeliveryDashboardAdapter

    private var pendingEntries: List<AdminDeliveryEntry> = emptyList()
    private var deliveredEntries: List<AdminDeliveryEntry> = emptyList()
    private var selectedKind = "all"
    private var selectedStatus = "all"
    private var selectedDate: String? = null

    private val kindOptions = linkedMapOf(
        "All deliveries" to "all",
        "Orders only" to "order",
        "Subscriptions only" to "subscription"
    )

    private val statusOptions = linkedMapOf(
        "All statuses" to "all",
        "Pending" to "pending",
        "Confirmed" to "confirmed",
        "Processing" to "processing",
        "Ready" to "ready",
        "Out for delivery" to "out_for_delivery",
        "Failed" to "failed",
        "Delivered" to "delivered"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdminUserDeliveryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAdminLayout("Delivery Dashboard")
        setupHeader()
        setupRecyclerView()
        setupTabs()
        setupFilters()

        binding.swipeRefresh.setOnRefreshListener { loadDashboard() }

        loadDashboard()
    }

    private fun setupHeader() {
        binding.tvHeadline.text = "Delivery command center"
        binding.tvSubHeadline.text =
            "Track pending drops, completed handoffs, and subscription billing from one admin view."
    }

    private fun setupRecyclerView() {
        adapter = AdminDeliveryDashboardAdapter { item, newStatus ->
            updateStatus(item, newStatus)
        }
        binding.rvDeliveries.layoutManager = LinearLayoutManager(this)
        binding.rvDeliveries.adapter = adapter
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                renderCurrentTab()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit

            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })
    }

    private fun setupFilters() {
        binding.autoKindFilter.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, kindOptions.keys.toList())
        )
        binding.autoStatusFilter.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, statusOptions.keys.toList())
        )

        binding.autoKindFilter.setText(kindOptions.keys.first(), false)
        binding.autoStatusFilter.setText(statusOptions.keys.first(), false)

        binding.autoKindFilter.setOnItemClickListener { _, _, position, _ ->
            selectedKind = kindOptions.values.elementAt(position)
        }
        binding.autoStatusFilter.setOnItemClickListener { _, _, position, _ ->
            selectedStatus = statusOptions.values.elementAt(position)
        }

        binding.btnDateFilter.setOnClickListener { showDatePicker() }
        binding.btnClearDate.setOnClickListener {
            selectedDate = null
            bindSelectedDate()
        }
        binding.btnApplyFilters.setOnClickListener { loadDashboard() }
        binding.btnResetFilters.setOnClickListener {
            selectedKind = "all"
            selectedStatus = "all"
            selectedDate = null
            binding.etSearchFilter.setText("")
            binding.autoKindFilter.setText(kindOptions.keys.first(), false)
            binding.autoStatusFilter.setText(statusOptions.keys.first(), false)
            bindSelectedDate()
            loadDashboard()
        }

        bindSelectedDate()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        selectedDate?.let {
            runCatching {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it) ?: return@runCatching
                calendar.time = parsed
            }
        }

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                bindSelectedDate()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun bindSelectedDate() {
        binding.btnDateFilter.text = selectedDate ?: "Any date"
    }

    private fun buildQueryMap(): Map<String, String> {
        val query = linkedMapOf<String, String>()
        val search = binding.etSearchFilter.text?.toString()?.trim().orEmpty()

        if (search.isNotEmpty()) query["q"] = search
        if (selectedKind != "all") query["kind"] = selectedKind
        if (selectedStatus != "all") query["status"] = selectedStatus
        selectedDate?.let { query["date"] = it }

        return query
    }

    private fun loadDashboard() {
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.adminDeliveryDashboardApi.getDeliveryDashboard(buildQueryMap())
                withContext(Dispatchers.Main) {
                    if (isDestroyed) return@withContext
                    binding.swipeRefresh.isRefreshing = false
                    bindResponse(response)
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    if (isDestroyed) return@withContext
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(
                        this@AdminUserDeliveryActivity,
                        NetworkMessageUtils.friendlyMessage(t, "Failed to load delivery dashboard"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun bindResponse(response: AdminDeliveryDashboardResponse) {
        pendingEntries = (response.pendingCustomerOrders + response.pendingSubscriptions)
            .sortedBy { it.deliveryDate ?: it.date ?: "" }
        deliveredEntries = (response.deliveredCustomerOrders + response.deliveredSubscriptions)
            .sortedByDescending { it.deliveredAt ?: it.deliveryDate ?: it.date ?: "" }

        binding.tvTodayLabel.text = formatDashboardDate(response.date)
        binding.tvPendingCount.text = response.summary.pendingTotal.toString()
        binding.tvDeliveredCount.text = response.summary.deliveredTotal.toString()
        binding.tvOrdersCount.text =
            (response.summary.pendingOrders + response.summary.deliveredOrders).toString()
        binding.tvSubscriptionsCount.text =
            (response.summary.pendingSubscriptions + response.summary.deliveredSubscriptions).toString()

        updateTabTitles()
        renderCurrentTab()
    }

    private fun updateTabTitles() {
        binding.tabLayout.getTabAt(0)?.text = "Pending (${pendingEntries.size})"
        binding.tabLayout.getTabAt(1)?.text = "Delivered (${deliveredEntries.size})"
    }

    private fun renderCurrentTab() {
        val showPending = binding.tabLayout.selectedTabPosition != 1
        val currentItems = if (showPending) pendingEntries else deliveredEntries

        adapter.submitList(currentItems)
        binding.rvDeliveries.visibility = if (currentItems.isEmpty()) View.GONE else View.VISIBLE
        binding.layoutEmpty.visibility = if (currentItems.isEmpty()) View.VISIBLE else View.GONE
        binding.tvEmptyTitle.text = if (showPending) "Nothing pending" else "No deliveries completed"
        binding.tvEmptyMessage.text = if (showPending) {
            "Adjust the filters or wait for fresh order and subscription drops."
        } else {
            "Delivered entries matching your filters will appear here."
        }
    }

    private fun updateStatus(item: AdminDeliveryEntry, newStatus: String) {
        val request = DeliveryUpdateRequest(
            type = item.type,
            deliveryId = item.deliveryId,
            orderId = item.orderId,
            subscriptionOrderId = item.subscriptionOrderId,
            status = newStatus,
            deliveredAmount = if (newStatus == "delivered" && item.type == "order") {
                item.grandTotal ?: item.totalAmount
            } else {
                null
            },
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
                        this@AdminUserDeliveryActivity,
                        response.body()?.message ?: "Delivery updated",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadDashboard()
                } else {
                    Toast.makeText(
                        this@AdminUserDeliveryActivity,
                        response.body()?.message ?: "Failed to update status",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<DeliveryUpdateResponse>, t: Throwable) {
                hideScreenLoading()
                Toast.makeText(
                    this@AdminUserDeliveryActivity,
                    NetworkMessageUtils.friendlyMessage(t, "Failed to update status"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun formatDashboardDate(value: String): String {
        return runCatching {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            formatter.format(parser.parse(value)!!)
        }.getOrDefault(value)
    }

    private fun currentIsoTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).format(Date())
    }
}
