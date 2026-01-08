package com.svd.svdagencies.ui.admin.items

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.AdminItem
import com.svd.svdagencies.ui.admin.Adapter.AdminItemAdapter
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminItemsActivity : AdminBaseActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvItems: RecyclerView
    private lateinit var etSearchItems: EditText
    private lateinit var btnAddItem: MaterialCardView
    private lateinit var tabLayoutCategories: TabLayout
    private lateinit var adapter: AdminItemAdapter

    private var currentCategory: String = "Milk" // Default category

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_items)

        setupAdminLayout("Items")

        swipeRefresh = findViewById(R.id.swipeRefresh)
        rvItems = findViewById(R.id.rvItems)
        etSearchItems = findViewById(R.id.etSearchItems)
        btnAddItem = findViewById(R.id.btnAddItem)
        tabLayoutCategories = findViewById(R.id.tabLayoutCategories)

        setupRecycler()
        setupListeners()
        loadCategories() // Load categories first, which will trigger loadItems
    }

    private fun setupRecycler() {
        adapter = AdminItemAdapter(
            emptyList(),
            onEditClick = { item -> 
                val intent = Intent(this, AddEditItemActivity::class.java)
                intent.putExtra("ITEM_TO_UPDATE", item)
                startActivity(intent)
            },
            onFreezeClick = { item -> showToast("Freeze Item: ${item.name} (Coming soon)") }
        )
        rvItems.layoutManager = LinearLayoutManager(this)
        rvItems.adapter = adapter
    }

    private fun setupListeners() {
        btnAddItem.setOnClickListener {
            startActivity(Intent(this, AddEditItemActivity::class.java))
        }

        tabLayoutCategories.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.text?.let { category ->
                    currentCategory = category.toString()
                    loadItems(currentCategory)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadCategories() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val categories = ApiClient.adminItemsApi.getCategories()
                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        setupTabs(categories)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        showToast("Failed to load categories: ${e.message}")
                        // Even if failed, try loading default "Milk" category
                        loadItems(currentCategory)
                    }
                }
            }
        }
    }

    private fun setupTabs(categories: List<String>) {
        tabLayoutCategories.removeAllTabs()
        
        // Add categories from API
        for (category in categories) {
            val tab = tabLayoutCategories.newTab().setText(category)
            tabLayoutCategories.addTab(tab)
            
            // Select default category "Milk" if present
            if (category.equals("Milk", ignoreCase = true)) {
                tab.select()
            }
        }

        // If no "Milk" found or list empty, select first tab if available
        if (tabLayoutCategories.tabCount > 0 && tabLayoutCategories.selectedTabPosition == -1) {
             tabLayoutCategories.getTabAt(0)?.select()
        } else if (tabLayoutCategories.selectedTabPosition != -1) {
             // Already selected (e.g. Milk), ensure we load items for it
             // Actually, selecting programmatically triggers onTabSelected which calls loadItems
             // But sometimes on initial add it might not trigger if added first. 
             // Let's verify: addTab doesn't select unless it's the first one and no other is selected?
             // Usually first tab is selected by default.
             // We can explicitly trigger load here if we want to be sure.
             // The OnTabSelectedListener will handle it if we call select().
        }
    }

    private fun loadItems(category: String = currentCategory) {
        swipeRefresh.isRefreshing = true
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val items = ApiClient.adminItemsApi.getItemsByCategory(category)
                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        swipeRefresh.isRefreshing = false
                        adapter.updateList(items)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        swipeRefresh.isRefreshing = false
                        showToast("Error loading items: ${e.message}")
                    }
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
