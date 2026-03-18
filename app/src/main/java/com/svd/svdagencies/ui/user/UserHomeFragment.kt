package com.svd.svdagencies.ui.user

import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.user.UserDashboardResponse
import com.svd.svdagencies.data.repository.UserDashboardObserver
import com.svd.svdagencies.data.repository.UserRepository
import com.svd.svdagencies.ui.user.adapter.ProductSliderAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class UserHomeFragment : Fragment(R.layout.user_home), UserDashboardObserver {

    private val cartViewModel: UserCartViewModel by activityViewModels()
    private lateinit var tvUserName: TextView
    private lateinit var ttvLocation: TextView
    private lateinit var chipGroupProductCategories: ChipGroup
    private lateinit var rvProductSlider: RecyclerView
    private val productAdapter = ProductSliderAdapter(
        onAddToCart = { item ->
            cartViewModel.addToCart(item)
            showToast("Added ${item.name} to cart")
        },
        onIncrease = { item -> cartViewModel.addToCart(item) },
        onDecrease = { item -> cartViewModel.updateQuantity(item.id, cartViewModel.getQuantity(item.id) - 1) },
        quantityProvider = { item -> cartViewModel.getQuantity(item.id) }
    )
    private var selectedProductCategory: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ttvLocation = view.findViewById(R.id.tvLocation)
        tvUserName = view.findViewById(R.id.tvUserName)
        chipGroupProductCategories = view.findViewById(R.id.chipGroupProductCategories)
        rvProductSlider = view.findViewById(R.id.rvProductSlider)

        rvProductSlider.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvProductSlider.adapter = productAdapter

        cartViewModel.cartItems.observe(viewLifecycleOwner) {
            productAdapter.refreshQuantities()
        }

        loadProductCategories()
    }

    override fun onStart() {
        super.onStart()
        UserRepository.registerObserver(this)
    }

    override fun onStop() {
        UserRepository.unregisterObserver(this)
        super.onStop()
    }

    override fun onDashboardUpdated(data: UserDashboardResponse) {
        updateUI(data)
    }

    private fun updateUI(data: UserDashboardResponse) {
        ttvLocation.text = data.customer.area
        tvUserName.text = data.customer.name
    }

    private fun loadProductCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            val categories = try {
                withContext(Dispatchers.IO) { ApiClient.adminItemsApi.getCategories() }
            } catch (t: Throwable) {
                showToast("Unable to load product categories")
                emptyList()
            }
            if (categories.isNotEmpty()) {
                populateCategoryChips(categories)
            }
        }
    }

    private fun populateCategoryChips(categories: List<String>) {
        val filtered = categories.filter { it.isNotBlank() }
        if (filtered.isEmpty()) return

        chipGroupProductCategories.removeAllViews()

        filtered.forEach { category ->
            val chip = Chip(requireContext()).apply {
                text = category.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                tag = category
                isCheckable = true
                setOnClickListener { selectProductCategory(category) }
            }

            chipGroupProductCategories.addView(chip)
        }

        val defaultCategory = filtered.firstOrNull { it.equals("milk", true) } ?: filtered.first()
        chipGroupProductCategories.children
            .filterIsInstance<Chip>()
            .firstOrNull { it.tag == defaultCategory }
            ?.isChecked = true
        selectProductCategory(defaultCategory)
    }

    private fun selectProductCategory(category: String) {
        if (selectedProductCategory == category) return
        selectedProductCategory = category

        viewLifecycleOwner.lifecycleScope.launch {
            val items = try {
                withContext(Dispatchers.IO) { ApiClient.adminItemsApi.getItemsByCategory(category) }
            } catch (t: Throwable) {
                showToast("Unable to load $category products")
                emptyList()
            }
            if (items.isEmpty()) showToast("No items under $category yet")
            productAdapter.submitList(items)
        }
    }

    private fun showToast(message: String) {
        if (!isAdded) return
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
