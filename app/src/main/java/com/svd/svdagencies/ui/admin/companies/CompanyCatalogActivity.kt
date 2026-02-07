package com.svd.svdagencies.ui.admin.companies

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.databinding.AdminCompanyCatalogBinding
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import kotlinx.coroutines.launch

class CompanyCatalogActivity : AdminBaseActivity() {
    private lateinit var binding: AdminCompanyCatalogBinding
    private lateinit var adapter: CatalogAdapter
    private var companyId: Int = -1
    private var companyName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdminCompanyCatalogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        companyId = intent.getIntExtra("COMPANY_ID", -1)
        companyName = intent.getStringExtra("COMPANY_NAME") ?: "Catalog"

        setupAdminLayout(companyName)
        setupRecyclerView()
        setupSwipeRefresh()

        if (companyId != -1) {
            fetchCatalog()
        } else {
            Toast.makeText(this, "Invalid Company", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = CatalogAdapter()
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
                val items = ApiClient.adminCompaniesApi.getCompanyItems(companyId)
                if (items.isEmpty()) {
                    binding.rvCatalogItems.visibility = View.GONE
                    binding.llEmptyState.visibility = View.VISIBLE
                } else {
                    binding.rvCatalogItems.visibility = View.VISIBLE
                    binding.llEmptyState.visibility = View.GONE
                    adapter.submitList(items)
                }
            } catch (e: Exception) {
                Toast.makeText(this@CompanyCatalogActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                binding.llEmptyState.visibility = View.VISIBLE
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
}
