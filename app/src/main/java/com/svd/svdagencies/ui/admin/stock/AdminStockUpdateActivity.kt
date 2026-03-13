package com.svd.svdagencies.ui.admin.stock

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.stock.AdminStockDashboardResponse
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import com.svd.svdagencies.utils.NetworkMessageUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminStockUpdateActivity : AdminBaseActivity() {
    
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvUpdateStock: RecyclerView
    private lateinit var btnSubmitUpdate: MaterialButton
    private lateinit var adapter: StockUpdateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_stock_update_activity)
        setupAdminLayout("Update Stock")

        swipeRefresh = findViewById(R.id.swipeRefresh)
        rvUpdateStock = findViewById(R.id.rvUpdateStock)
        btnSubmitUpdate = findViewById(R.id.btnSubmitUpdate)

        adapter = StockUpdateAdapter(emptyList())
        rvUpdateStock.layoutManager = LinearLayoutManager(this)
        rvUpdateStock.adapter = adapter

        swipeRefresh.setOnRefreshListener {
            loadStockItems()
        }

        btnSubmitUpdate.setOnClickListener {
            submitStockUpdates()
        }

        loadStockItems()
    }

    private fun loadStockItems() {
        swipeRefresh.isRefreshing = true
        ApiClient.adminStockApi.getStockDashboard().enqueue(object : Callback<AdminStockDashboardResponse> {
            override fun onResponse(
                call: Call<AdminStockDashboardResponse>,
                response: Response<AdminStockDashboardResponse>
            ) {
                swipeRefresh.isRefreshing = false
                if (response.isSuccessful && response.body() != null) {
                    adapter.updateList(response.body()!!.allItems)
                } else {
                    Toast.makeText(this@AdminStockUpdateActivity, "Failed to load items", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AdminStockDashboardResponse>, t: Throwable) {
                swipeRefresh.isRefreshing = false
                Toast.makeText(
                    this@AdminStockUpdateActivity,
                    NetworkMessageUtils.friendlyMessage(t, "Failed to load items"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun submitStockUpdates() {
        val updates = adapter.getUpdates()
        if (updates.isEmpty()) {
            Toast.makeText(this, "No changes to update", Toast.LENGTH_SHORT).show()
            return
        }

        btnSubmitUpdate.isEnabled = false
        val body = mapOf("updates" to updates)

        ApiClient.adminStockApi.updateStock(body).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                btnSubmitUpdate.isEnabled = true
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminStockUpdateActivity, "Stock updated successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@AdminStockUpdateActivity, AdminStockActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@AdminStockUpdateActivity, "Failed to update stock", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                btnSubmitUpdate.isEnabled = true
                Toast.makeText(
                    this@AdminStockUpdateActivity,
                    NetworkMessageUtils.friendlyMessage(t, "Failed to update stock"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}
