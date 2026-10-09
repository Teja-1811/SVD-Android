package com.svd.svdagencies.ui.delivery

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.svd.svdagencies.R
import com.svd.svdagencies.base.BaseActivity
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.databinding.DeliveryCatalogBinding
import com.svd.svdagencies.ui.customer.adapter.CustomerCatalogAdapter
import kotlinx.coroutines.launch

class DeliveryCatalogActivity : BaseActivity() {
    private lateinit var binding: DeliveryCatalogBinding
    private lateinit var adapter: CustomerCatalogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DeliveryCatalogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        DeliveryNavigation.setup(
            this,
            binding.deliveryDrawerLayout,
            binding.deliveryNavigationView,
            toolbar = binding.toolbar,
            selectedItemId = R.id.nav_delivery_catalog
        )

        adapter = CustomerCatalogAdapter()
        binding.rvCatalogItems.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { fetchCatalog() }

        fetchCatalog()
    }

    private fun fetchCatalog() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val response = ApiClient.productApi.getCustomerCatalog(companyId = null)
                val products = response.catalog.flatMap { it.products }
                binding.rvCatalogItems.visibility = if (products.isEmpty()) View.GONE else View.VISIBLE
                binding.llEmptyState.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
                adapter.submitList(products)
            } catch (e: Exception) {
                binding.rvCatalogItems.visibility = View.GONE
                binding.llEmptyState.visibility = View.VISIBLE
                Toast.makeText(this@DeliveryCatalogActivity, "Failed to load catalog", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
}
