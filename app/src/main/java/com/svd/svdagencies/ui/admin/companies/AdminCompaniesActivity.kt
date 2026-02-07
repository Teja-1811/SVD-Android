package com.svd.svdagencies.ui.admin.companies

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.Company
import com.svd.svdagencies.databinding.AdminCompaniesBinding
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import kotlinx.coroutines.launch

class AdminCompaniesActivity : AdminBaseActivity() {
    private lateinit var binding: AdminCompaniesBinding
    private lateinit var adapter: CompaniesAdapter
    private var allCompanies = listOf<Company>()

    private val addEditLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            fetchCompanies()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdminCompaniesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupAdminLayout("Companies")

        setupRecyclerView()
        setupSearch()
        setupSwipeRefresh()
        setupAddButton()

        fetchCompanies()
    }

    private fun setupRecyclerView() {
        adapter = CompaniesAdapter(
            onEditClick = { company ->
                val intent = Intent(this, AddEditCompanyActivity::class.java)
                intent.putExtra("COMPANY_TO_UPDATE", company)
                addEditLauncher.launch(intent)
            },
            onViewCatalogClick = { company ->
                val intent = Intent(this, CompanyCatalogActivity::class.java)
                intent.putExtra("COMPANY_ID", company.id)
                intent.putExtra("COMPANY_NAME", company.name)
                startActivity(intent)
            }
        )
        binding.rvCompanies.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearchCompanies.addTextChangedListener { text ->
            val query = text.toString().lowercase()
            val filtered = allCompanies.filter { 
                it.name.lowercase().contains(query)
            }
            adapter.submitList(filtered)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            fetchCompanies()
        }
    }

    private fun setupAddButton() {
        binding.btnAddCompany.setOnClickListener {
            val intent = Intent(this, AddEditCompanyActivity::class.java)
            addEditLauncher.launch(intent)
        }
    }

    private fun fetchCompanies() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val response = ApiClient.adminCompaniesApi.getCompanies()
                allCompanies = response.companies
                adapter.submitList(allCompanies)
            } catch (e: Exception) {
                Toast.makeText(this@AdminCompaniesActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
}
