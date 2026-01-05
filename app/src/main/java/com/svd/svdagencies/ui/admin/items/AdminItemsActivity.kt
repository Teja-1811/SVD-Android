package com.svd.svdagencies.ui.admin.items

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.card.MaterialCardView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.admin.AdminItem
import com.svd.svdagencies.ui.admin.Adapter.AdminItemAdapter
import com.svd.svdagencies.ui.admin.AdminBaseActivity

class AdminItemsActivity : AdminBaseActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvItems: RecyclerView
    private lateinit var etSearchItems: EditText
    private lateinit var btnAddItem: MaterialCardView
    private lateinit var tvCategoryTitle: TextView
    private lateinit var adapter: AdminItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_items)

        setupAdminLayout("Items")

        swipeRefresh = findViewById(R.id.swipeRefresh)
        rvItems = findViewById(R.id.rvItems)
        etSearchItems = findViewById(R.id.etSearchItems)
        btnAddItem = findViewById(R.id.btnAddItem)
        tvCategoryTitle = findViewById(R.id.tvCategoryTitle)

        setupRecycler()
        setupListeners()

        swipeRefresh.setOnRefreshListener {
            loadItems()
        }

        // Initially load data
        loadItems()
    }

    private fun setupRecycler() {
        adapter = AdminItemAdapter(
            emptyList(),
            onEditClick = { item -> showToast("Edit Item: ${item.name}") },
            onFreezeClick = { item -> showToast("Freeze Item: ${item.name}") }
        )
        rvItems.layoutManager = LinearLayoutManager(this)
        rvItems.adapter = adapter
    }

    private fun setupListeners() {
        btnAddItem.setOnClickListener {
            showToast("Add Item Clicked")
        }
    }

    private fun loadItems() {
        swipeRefresh.isRefreshing = false

        // Mock data matching the screenshot structure
        val mockItems = listOf(
            AdminItem(
                id = 1,
                name = "Full Cream Milk 120 ML",
                code = "FCM120",
                company = "Dodla",
                buying_price = 8.16,
                selling_price = 9.00,
                mrp = 10.00,
                margin = 0.84,
                stock = 57,
                category = "Milk"
            ),
            AdminItem(
                id = 2,
                name = "Toned Milk 500 ML",
                code = "TM500",
                company = "Heritage",
                buying_price = 22.50,
                selling_price = 24.00,
                mrp = 26.00,
                margin = 1.50,
                stock = 35,
                category = "Milk"
            )
        )

        adapter.updateList(mockItems)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}