package com.svd.svdagencies.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.Delivery
import com.svd.svdagencies.databinding.AdminUserDeliveryBinding
import com.svd.svdagencies.ui.admin.adapter.DeliveryAdapter
import com.svd.svdagencies.utils.NetworkMessageUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminUserDeliveryActivity : AdminBaseActivity() {

    private lateinit var binding: AdminUserDeliveryBinding
    private lateinit var adapter: DeliveryAdapter
    private var allDeliveries: List<Delivery> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdminUserDeliveryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAdminLayout("Today's Deliveries")
        setupRecyclerView()

        binding.swipeRefresh.setOnRefreshListener {
            loadDeliveries()
        }

        setupSearch()
        loadDeliveries()
    }

    private fun setupRecyclerView() {
        adapter = DeliveryAdapter(emptyList())
        binding.rvTodayDeliveries.layoutManager = LinearLayoutManager(this)
        binding.rvTodayDeliveries.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearchDelivery.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterDeliveries(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun loadDeliveries() {
        binding.swipeRefresh.isRefreshing = true
        ApiClient.subscriptionApi.getTodayDeliveries().enqueue(object : Callback<List<Delivery>> {
            override fun onResponse(call: Call<List<Delivery>>, response: Response<List<Delivery>>) {
                binding.swipeRefresh.isRefreshing = false
                if (response.isSuccessful) {
                    allDeliveries = response.body() ?: emptyList()
                    filterDeliveries(binding.etSearchDelivery.text?.toString().orEmpty())
                } else {
                    Toast.makeText(this@AdminUserDeliveryActivity, "Failed to load deliveries", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Delivery>>, t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(
                    this@AdminUserDeliveryActivity,
                    NetworkMessageUtils.friendlyMessage(t, "Failed to load deliveries"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun filterDeliveries(query: String) {
        val trimmedQuery = query.trim()
        val filteredDeliveries = if (trimmedQuery.isEmpty()) {
            allDeliveries
        } else {
            allDeliveries.filter { delivery ->
                delivery.customer.contains(trimmedQuery, ignoreCase = true) ||
                    delivery.phone.contains(trimmedQuery, ignoreCase = true) ||
                    delivery.plan.contains(trimmedQuery, ignoreCase = true)
            }
        }

        adapter.updateData(filteredDeliveries)
        val hasItems = filteredDeliveries.isNotEmpty()
        binding.tvEmptyState.visibility = if (hasItems) View.GONE else View.VISIBLE
        binding.rvTodayDeliveries.visibility = if (hasItems) View.VISIBLE else View.GONE
    }
}
