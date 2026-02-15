package com.svd.svdagencies.ui.customer.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.Company
import com.svd.svdagencies.databinding.CustomerCompaniesBinding
import com.svd.svdagencies.ui.customer.CustomerCompanyCatalogActivity
import com.svd.svdagencies.ui.customer.adapter.CustomerCompaniesAdapter
import kotlinx.coroutines.launch

class CustomerCompaniesFragment : Fragment() {

    private var _binding: CustomerCompaniesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: CustomerCompaniesAdapter
    private var allCompanies = listOf<Company>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CustomerCompaniesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        setupSwipeRefresh()

        fetchCompanies()
    }

    private fun setupRecyclerView() {
        adapter = CustomerCompaniesAdapter {
            val intent = Intent(requireContext(), CustomerCompanyCatalogActivity::class.java)
            intent.putExtra("COMPANY_ID", it.id)
            intent.putExtra("COMPANY_NAME", it.name)
            startActivity(intent)
        }
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

    private fun fetchCompanies() {
        binding.swipeRefresh.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.customerApi.getCompanies()
                allCompanies = response.companies
                _binding?.let { 
                    adapter.submitList(allCompanies)
                }
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
