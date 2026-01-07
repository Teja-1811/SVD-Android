package com.svd.svdagencies.ui.admin.customer

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.admin.CustomerDashboardApi
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.CustomerDashboardResponse
import com.svd.svdagencies.data.model.admin.CustomerItem
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import com.svd.svdagencies.ui.admin.Adapter.CustomerAdapter
import com.svd.svdagencies.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CustomersData : AdminBaseActivity() {

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var rvCustomers: androidx.recyclerview.widget.RecyclerView
    private lateinit var adapter: CustomerAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var api: CustomerDashboardApi

    private lateinit var etSearch: EditText
    private lateinit var fabAddCustomer: FloatingActionButton
    private lateinit var tvEmptyState: TextView

    private var allCustomers: List<CustomerItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_customers)

        // Setup shared admin UI
        setupAdminLayout("Customers")

        sessionManager = SessionManager(this)
        api = ApiClient.adminCustomerDashboard

        initViews()
        setupListeners()

        swipeRefreshLayout.isRefreshing = true
        loadCustomers()
    }

    private fun initViews() {

        swipeRefreshLayout = findViewById(R.id.swipeRefresh)
        rvCustomers = findViewById(R.id.rvCustomers)
        etSearch = findViewById(R.id.etSearch)
        fabAddCustomer = findViewById(R.id.fabAddCustomer)
        tvEmptyState = findViewById(R.id.tvEmptyState) // Add this in XML

        rvCustomers.layoutManager = LinearLayoutManager(this)
        adapter = CustomerAdapter(emptyList())
        rvCustomers.adapter = adapter
    }

    private fun setupListeners() {

        swipeRefreshLayout.setOnRefreshListener {
            loadCustomers()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterCustomers(s?.toString().orEmpty())
            }
        })

        fabAddCustomer.setOnClickListener {
            val intent = Intent(this, AddCustomerActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadCustomers() {

        swipeRefreshLayout.isRefreshing = true

        api.getCustomers().enqueue(object : Callback<CustomerDashboardResponse> {

            override fun onResponse(
                call: Call<CustomerDashboardResponse>,
                response: Response<CustomerDashboardResponse>
            ) {
                if (isDestroyed) return
                swipeRefreshLayout.isRefreshing = false

                when {
                    response.isSuccessful -> {

                        allCustomers = response.body()?.customers ?: emptyList()

                        adapter.update(allCustomers)

                        val currentQuery = etSearch.text.toString().trim()
                        if (currentQuery.isNotEmpty()) filterCustomers(currentQuery)

                        showEmptyState(allCustomers.isEmpty())
                    }

                    response.code() == 401 -> {
                        sessionManager.logout()
                    }

                    else -> {
                        Toast.makeText(this@CustomersData, "Failed to load customers", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<CustomerDashboardResponse>, t: Throwable) {
                if (isDestroyed) return
                swipeRefreshLayout.isRefreshing = false

                Toast.makeText(
                    this@CustomersData,
                    "Please check your internet connection",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun filterCustomers(query: String) {

        val q = query.trim()

        val filtered = if (q.isEmpty()) allCustomers else
            allCustomers.filter { item ->
                listOfNotNull(item.name, item.shop_name, item.phone)
                    .any { it.contains(q, ignoreCase = true) }
            }

        adapter.update(filtered)

        showEmptyState(filtered.isEmpty())
    }

    private fun showEmptyState(show: Boolean) {
        tvEmptyState.visibility = if (show) View.VISIBLE else View.GONE
        rvCustomers.visibility = if (show) View.GONE else View.VISIBLE
    }
}
