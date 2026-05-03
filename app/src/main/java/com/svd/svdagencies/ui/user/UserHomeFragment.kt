package com.svd.svdagencies.ui.user

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.children
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.user.UserDashboardResponse
import com.svd.svdagencies.data.model.user.UserOffer
import com.svd.svdagencies.data.repository.UserDashboardObserver
import com.svd.svdagencies.data.repository.UserRepository
import com.svd.svdagencies.ui.user.adapter.ProductSliderAdapter
import com.svd.svdagencies.ui.user.adapter.UserOfferAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class UserHomeFragment : Fragment(R.layout.user_home), UserDashboardObserver {

    private val cartViewModel: UserCartViewModel by activityViewModels()
    private lateinit var scrollView: NestedScrollView
    private lateinit var tvUserName: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvAccountBadge: TextView
    private lateinit var tvSubscriptionHint: TextView
    private lateinit var tvOffersCount: TextView
    private lateinit var tvOffersStatus: TextView
    private lateinit var btnAddOrder: MaterialButton
    private lateinit var btnUseOffers: MaterialButton
    private lateinit var btnBuySubscription: MaterialButton
    private lateinit var chipGroupProductCategories: ChipGroup
    private lateinit var rvOffers: RecyclerView
    private lateinit var rvProductSlider: RecyclerView

    private val offerAdapter = UserOfferAdapter(onUseOffer = { offer -> showOfferGuide(offer) })
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

        scrollView = view.findViewById(R.id.userHomeScroll)
        tvLocation = view.findViewById(R.id.tvLocation)
        tvUserName = view.findViewById(R.id.tvUserName)
        tvAccountBadge = view.findViewById(R.id.tvAccountBadge)
        tvSubscriptionHint = view.findViewById(R.id.tvSubscriptionHint)
        tvOffersCount = view.findViewById(R.id.tvOffersCount)
        tvOffersStatus = view.findViewById(R.id.tvOffersStatus)
        btnAddOrder = view.findViewById(R.id.btnAddOrder)
        btnUseOffers = view.findViewById(R.id.btnUseOffers)
        btnBuySubscription = view.findViewById(R.id.btnBuySubscription)
        chipGroupProductCategories = view.findViewById(R.id.chipGroupProductCategories)
        rvOffers = view.findViewById(R.id.rvOffers)
        rvProductSlider = view.findViewById(R.id.rvProductSlider)

        rvOffers.layoutManager = LinearLayoutManager(requireContext())
        rvOffers.adapter = offerAdapter

        rvProductSlider.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvProductSlider.adapter = productAdapter

        cartViewModel.cartItems.observe(viewLifecycleOwner) {
            productAdapter.refreshQuantities()
        }

        btnAddOrder.setOnClickListener {
            (activity as? UserMainActivity)?.navigateToTab(R.id.nav_cart)
        }
        btnUseOffers.setOnClickListener {
            scrollToSection(rvOffers)
        }
        btnBuySubscription.setOnClickListener {
            (activity as? UserMainActivity)?.navigateToTab(R.id.nav_subscription)
        }

        observeViewModel()
        UserRepository.getCachedDashboard()?.let { updateUI(it) }
        cartViewModel.loadCategories { showToast(it) }
    }

    private fun observeViewModel() {
        cartViewModel.categories.observe(viewLifecycleOwner) { categories ->
            if (categories.isNotEmpty()) {
                populateCategoryChips(categories)
            }
        }

        cartViewModel.itemsBySelectedCategory.observe(viewLifecycleOwner) { items ->
            productAdapter.submitList(items)
        }
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
        tvLocation.text = data.customer.area?.takeIf { it.isNotBlank() } ?: "Service area"
        tvUserName.text = data.customer.name
        val hasSubscription = !data.subscription.planName.isNullOrBlank() &&
            data.subscription.planName != "No active subscription"
        val activeOffers = data.offers

        tvAccountBadge.text = if (hasSubscription) "SUBSCRIBED" else "READY"
        tvSubscriptionHint.text = if (hasSubscription) {
            "Subscription active. Review offers or add items to your next order."
        } else {
            "No active subscription yet. Explore offers and buy a plan to get started."
        }

        tvOffersCount.text = "${activeOffers.size} live"
        tvOffersStatus.visibility = View.VISIBLE
        tvOffersStatus.text = if (activeOffers.isEmpty()) {
            "No live offers right now. Pull to refresh and check again later."
        } else {
            "Use an offer, then add the matching products below to make the deal count."
        }
        offerAdapter.submitList(activeOffers)
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
        cartViewModel.loadItemsByCategory(category) { showToast(it) }
    }

    private fun showOfferGuide(offer: UserOffer) {
        val itemSummary = if (offer.items.isEmpty()) {
            "This offer is live now. Add the eligible items from the product list below to use it in your next order."
        } else {
            offer.items.joinToString(separator = "\n") {
                "${it.itemName}: buy ${it.buyQty} and get ${it.offerQty}"
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(offer.name)
            .setMessage(itemSummary)
            .setPositiveButton("Shop Products") { _, _ ->
                scrollToSection(rvProductSlider)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun scrollToSection(target: View) {
        target.post {
            scrollView.smoothScrollTo(0, target.top)
        }
    }

    private fun showToast(message: String) {
        if (!isAdded) return
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
