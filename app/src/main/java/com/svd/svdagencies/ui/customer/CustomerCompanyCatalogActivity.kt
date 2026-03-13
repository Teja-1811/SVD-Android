package com.svd.svdagencies.ui.customer

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.databinding.CustomerCompanyCatalogBinding
import com.svd.svdagencies.ui.customer.adapter.CustomerCatalogAdapter
import kotlinx.coroutines.launch

class CustomerCompanyCatalogActivity : AppCompatActivity() {
    private lateinit var binding: CustomerCompanyCatalogBinding
    private lateinit var adapter: CustomerCatalogAdapter
    private var companyId: Int = -1
    private var companyName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CustomerCompanyCatalogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        companyId = intent.getIntExtra("COMPANY_ID", -1)
        companyName = intent.getStringExtra("COMPANY_NAME") ?: "Catalog"

        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()

        if (companyId != -1) {
            fetchCatalog()
        } else {
            Toast.makeText(this, "Invalid Company", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = companyName
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = CustomerCatalogAdapter()
        binding.rvCatalogItems.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            fetchCatalog()
        }
    }

    private fun fetchCatalog() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val response = ApiClient.productApi.getCustomerCatalog(companyId)
                
                // The API returns a CatalogResponse which contains categories.
                // For a specific company catalog, we flatten the products from all categories.
                val allProducts = response.catalog.flatMap { it.products }
                
                if (allProducts.isEmpty()) {
                    binding.rvCatalogItems.visibility = View.GONE
                    binding.llEmptyState.visibility = View.VISIBLE
                } else {
                    binding.rvCatalogItems.visibility = View.VISIBLE
                    binding.llEmptyState.visibility = View.GONE
                    adapter.submitList(allProducts)
                }
            } catch (e: Exception) {
                Toast.makeText(this@CustomerCompanyCatalogActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                binding.llEmptyState.visibility = View.VISIBLE
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
}
