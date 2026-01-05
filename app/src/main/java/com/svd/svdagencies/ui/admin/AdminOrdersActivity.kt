package com.svd.svdagencies.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.svd.svdagencies.R

class AdminOrdersActivity : AdminBaseActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvOrders: RecyclerView
    private lateinit var layoutNoOrders: LinearLayout
    private lateinit var tvOrderCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_orders)

        setupAdminLayout("Orders")

        swipeRefresh = findViewById(R.id.swipeRefresh)
        rvOrders = findViewById(R.id.rvOrders)
        layoutNoOrders = findViewById(R.id.layoutNoOrders)
        tvOrderCount = findViewById(R.id.tvOrderCount)

        setupRecycler()

        swipeRefresh.setOnRefreshListener {
            loadOrders()
        }

        loadOrders()
    }

    private fun setupRecycler() {
        rvOrders.layoutManager = LinearLayoutManager(this)
        // Adapter will be set when data is loaded
    }

    private fun loadOrders() {
        // Mock loading empty state for now as requested
        swipeRefresh.isRefreshing = false
        
        val orders: List<Any> = emptyList() // Replace with actual model later

        if (orders.isEmpty()) {
            layoutNoOrders.visibility = View.VISIBLE
            swipeRefresh.visibility = View.GONE
            tvOrderCount.text = "0"
        } else {
            layoutNoOrders.visibility = View.GONE
            swipeRefresh.visibility = View.VISIBLE
            tvOrderCount.text = orders.size.toString()
            // rvOrders.adapter = AdminOrdersAdapter(orders) 
        }
    }
}
