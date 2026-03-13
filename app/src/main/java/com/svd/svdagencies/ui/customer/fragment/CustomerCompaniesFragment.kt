package com.svd.svdagencies.ui.customer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.customer.CategoryData
import com.svd.svdagencies.R
import com.svd.svdagencies.databinding.CustomerCompaniesBinding
import com.svd.svdagencies.ui.customer.adapter.CustomerCatalogAdapter
import kotlinx.coroutines.launch

class CustomerCompaniesFragment : Fragment() {

    private var _binding: CustomerCompaniesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: CustomerCatalogAdapter
    private var categories = listOf<CategoryData>()

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
        setupSwipeRefresh()

        fetchCatalog()
    }

    private fun setupRecyclerView() {
        adapter = CustomerCatalogAdapter()
        binding.rvItems.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            fetchCatalog()
        }
    }

    private fun fetchCatalog() {
        binding.swipeRefresh.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.productApi.getCustomerCatalog(null)
                categories = response.catalog
                
                updateTabs()
                
                // Show "All" products by default
                adapter.submitList(categories.flatMap { it.products })
                
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun updateTabs() {
        val context = context ?: return
        binding.chipGroupCategories.removeAllViews()

        val allChip = createCategoryChip(context, "All") {
            adapter.submitList(categories.flatMap { it.products })
        }
        binding.chipGroupCategories.addView(allChip)
        allChip.isChecked = true

        categories.forEach { category ->
            val chip = createCategoryChip(context, category.categoryName) {
                adapter.submitList(category.products)
            }
            binding.chipGroupCategories.addView(chip)
        }
    }

    private fun createCategoryChip(
        context: android.content.Context,
        label: String,
        onSelected: () -> Unit
    ): Chip {
        return Chip(context).apply {
            text = label
            isCheckable = true
            isClickable = true
            chipMinHeight = 40f
            chipCornerRadius = 20f
            chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.chip_background_color)
            setTextColor(ContextCompat.getColorStateList(context, R.color.chip_text_color))
            chipStrokeColor = ContextCompat.getColorStateList(context, R.color.chip_unselected_border)
            chipStrokeWidth = 1f
            checkedIcon = null
            setEnsureMinTouchTargetSize(false)
            setOnClickListener { onSelected() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
