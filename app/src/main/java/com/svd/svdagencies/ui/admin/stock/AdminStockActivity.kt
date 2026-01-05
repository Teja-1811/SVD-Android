package com.svd.svdagencies.ui.admin.stock

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.card.MaterialCardView
import com.svd.svdagencies.R
import com.svd.svdagencies.ui.admin.AdminBaseActivity

class AdminStockActivity : AdminBaseActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvTotalItems: TextView
    private lateinit var tvTotalValue: TextView
    private lateinit var tvLowStock: TextView
    private lateinit var tvMovementIn: TextView
    private lateinit var tvMovementOut: TextView
    private lateinit var btnEditStock: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_stock)

        setupAdminLayout("Stock")

        swipeRefresh = findViewById(R.id.swipeRefresh)
        tvTotalItems = findViewById(R.id.tvTotalItems)
        tvTotalValue = findViewById(R.id.tvTotalValue)
        tvLowStock = findViewById(R.id.tvLowStock)
        tvMovementIn = findViewById(R.id.tvMovementIn)
        tvMovementOut = findViewById(R.id.tvMovementOut)
        btnEditStock = findViewById(R.id.btnEditStock)

        swipeRefresh.setOnRefreshListener {
            loadStockData()
        }

        btnEditStock.setOnClickListener {
            Toast.makeText(this, "Edit Stock Clicked", Toast.LENGTH_SHORT).show()
        }

        loadStockData()
    }

    private fun loadStockData() {
        swipeRefresh.isRefreshing = false

        // Mock data matching the screenshot
        tvTotalItems.text = "33"
        tvTotalValue.text = "₹ 9,774"
        tvLowStock.text = "27"
        tvMovementIn.text = "In: 0"
        tvMovementOut.text = "Out: 2,766"
    }
}