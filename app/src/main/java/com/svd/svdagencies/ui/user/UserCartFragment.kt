package com.svd.svdagencies.ui.user

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.ui.user.adapter.ProductSliderAdapter
import com.svd.svdagencies.ui.user.adapter.UserCartAdapter
import com.svd.svdagencies.ui.user.adapter.PendingOrdersAdapter
import com.svd.svdagencies.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.time.LocalDate
import android.app.DatePickerDialog
import androidx.lifecycle.Observer
import android.widget.ProgressBar

class UserCartFragment : Fragment(R.layout.user_cart) {

    private val cartViewModel: UserCartViewModel by activityViewModels()
    private lateinit var session: SessionManager
    private lateinit var tvEmptyCart: TextView
    private lateinit var tvNoPendingOrders: TextView
    private lateinit var tvCartTotal: TextView
    private lateinit var btnCheckout: MaterialButton
    private lateinit var switchPrebook: MaterialSwitch
    private lateinit var etPrebookDate: TextInputEditText
    private lateinit var cardCartSummary: MaterialCardView
    private lateinit var chipGroupCartCategories: ChipGroup
    private lateinit var rvCartProducts: RecyclerView
    private lateinit var rvPendingOrders: RecyclerView
    private lateinit var pbPendingOrders: ProgressBar
    private val pendingOrdersAdapter = PendingOrdersAdapter(
        onUpdate = { orderId, items, deliveryDate ->
            cartViewModel.updatePendingOrder(orderId, items, deliveryDate) { success, message ->
                showToast(message)
            }
        },
        onDelete = { orderId ->
            cartViewModel.deletePendingOrder(orderId) { success, message ->
                showToast(message)
            }
        }
    )
    private val productAdapter = ProductSliderAdapter(
        onAddToCart = { item ->
            cartViewModel.addToCart(item)
            showToast("Added ${item.name} to cart")
        },
        onIncrease = { item -> cartViewModel.addToCart(item) },
        onDecrease = { item -> cartViewModel.updateQuantity(item.id, cartViewModel.getQuantity(item.id) - 1) },
        quantityProvider = { item -> cartViewModel.getQuantity(item.id) }
    )
    private var selectedCategory: String? = null
    private var prebookEnabled = false
    private var prebookDate: LocalDate? = null
    private var checkoutDialog: BottomSheetDialog? = null
    private var checkoutAdapter: UserCartAdapter? = null
    private var checkoutObserver: Observer<List<com.svd.svdagencies.ui.user.model.CartItem>>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        session = SessionManager(requireContext())
        tvEmptyCart = view.findViewById(R.id.tvEmptyCart)
        tvNoPendingOrders = view.findViewById(R.id.tvNoPendingOrders)
        tvCartTotal = view.findViewById(R.id.tvCartTotal)
        btnCheckout = view.findViewById(R.id.btnCheckout)
        switchPrebook = view.findViewById(R.id.switchPrebook)
        etPrebookDate = view.findViewById(R.id.etPrebookDate)
        cardCartSummary = view.findViewById(R.id.cardCartSummary)
        chipGroupCartCategories = view.findViewById(R.id.chipGroupCartCategories)
        rvCartProducts = view.findViewById(R.id.rvCartProducts)
        rvPendingOrders = view.findViewById(R.id.rvPendingOrders)
        pbPendingOrders = view.findViewById(R.id.pbPendingOrders)

        rvCartProducts.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvCartProducts.adapter = productAdapter

        rvPendingOrders.layoutManager = LinearLayoutManager(requireContext())
        rvPendingOrders.adapter = pendingOrdersAdapter

        cartViewModel.setAllowOutOfStock(false)

        cartViewModel.cartItems.observe(viewLifecycleOwner) { items ->
            val hasItems = items.isNotEmpty()
            cardCartSummary.visibility = if (hasItems) View.VISIBLE else View.GONE
            tvEmptyCart.visibility = if (hasItems) View.GONE else View.VISIBLE
            productAdapter.refreshQuantities()
        }

        cartViewModel.totalAmount.observe(viewLifecycleOwner) { total ->
            tvCartTotal.text = "\u20B9${String.format("%.2f", total)}"
        }

        cartViewModel.itemCount.observe(viewLifecycleOwner) { count ->
            btnCheckout.text = if (count > 0) "Checkout ($count)" else "Checkout"
        }

        btnCheckout.setOnClickListener { showCheckoutModal() }

        switchPrebook.setOnCheckedChangeListener { _, isChecked ->
            prebookEnabled = isChecked
            togglePrebookMode(isChecked)
        }

        etPrebookDate.setOnClickListener { showDatePicker() }

        observeCategories()
        observePendingOrders()
        
        cartViewModel.loadCategories { showToast(it) }
        cartViewModel.loadPendingOrders()
    }

    private fun observeCategories() {
        cartViewModel.categories.observe(viewLifecycleOwner) { categories ->
            populateCategoryChips(categories)
        }
        
        cartViewModel.itemsBySelectedCategory.observe(viewLifecycleOwner) { items ->
            productAdapter.submitList(items)
        }
    }

    private fun populateCategoryChips(categories: List<String>) {
        val filtered = categories.filter { it.isNotBlank() }
        chipGroupCartCategories.removeAllViews()
        if (filtered.isEmpty()) return

        filtered.forEach { category ->
            val chip = Chip(requireContext()).apply {
                text = category.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                tag = category
                isCheckable = true
                setOnClickListener { selectCategory(category) }
            }
            chipGroupCartCategories.addView(chip)
        }

        val defaultCategory = filtered.firstOrNull { it.equals("milk", true) } ?: filtered.first()
        chipGroupCartCategories.children
            .filterIsInstance<Chip>()
            .firstOrNull { it.tag == defaultCategory }
            ?.isChecked = true
        selectCategory(defaultCategory)
    }

    private fun selectCategory(category: String) {
        if (selectedCategory == category) return
        selectedCategory = category
        cartViewModel.loadItemsByCategory(category) { showToast(it) }
    }

    private fun showToast(message: String) {
        if (!isAdded) return
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun togglePrebookMode(enabled: Boolean) {
        etPrebookDate.visibility = if (enabled) View.VISIBLE else View.GONE
        if (enabled && prebookDate == null) showDatePicker() else if (!enabled) prebookDate = null
        productAdapter.setAllowOutOfStock(enabled)
        cartViewModel.setAllowOutOfStock(enabled)
        checkoutAdapter?.setAllowOutOfStock(enabled)
    }

    private fun showDatePicker() {
        val base = prebookDate ?: LocalDate.now().plusDays(1)
        val picker = DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                prebookDate = LocalDate.of(y, m + 1, d)
                etPrebookDate.setText(prebookDate.toString())
            },
            base.year, base.monthValue - 1, base.dayOfMonth
        )
        picker.datePicker.minDate = System.currentTimeMillis()
        picker.show()
    }

    private fun showCheckoutModal() {
        val items = cartViewModel.cartItems.value.orEmpty()
        if (items.isEmpty()) {
            showToast("Your cart is empty")
            return
        }
        checkoutObserver?.let { cartViewModel.cartItems.removeObserver(it) }
        checkoutDialog?.dismiss()

        // Use the app's custom theme to ensure surfaceTint is removed (fixing the pink background)
        val dialog = BottomSheetDialog(requireContext(), R.style.ThemeOverlay_App_BottomSheetDialog)
        val view = layoutInflater.inflate(R.layout.user_checkout_bottom_sheet, null)
        val rv = view.findViewById<RecyclerView>(R.id.rvCheckoutSummary)
        val btnConfirm = view.findViewById<MaterialButton>(R.id.btnConfirmCheckout)
        val btnClose = view.findViewById<View>(R.id.btnCloseSheet)

        rv.layoutManager = LinearLayoutManager(requireContext())
        val modalAdapter = UserCartAdapter(
            onQuantityChanged = { id, qty -> cartViewModel.updateQuantity(id, qty) },
            onRemove = { id -> cartViewModel.removeItem(id) }
        ).also { it.setAllowOutOfStock(prebookEnabled) }
        rv.adapter = modalAdapter
        modalAdapter.submitList(items)
        checkoutAdapter = modalAdapter

        val observer = Observer<List<com.svd.svdagencies.ui.user.model.CartItem>> { list ->
            checkoutAdapter?.submitList(list)
            if (list.isEmpty()) dialog.dismiss()
        }
        checkoutObserver = observer
        cartViewModel.cartItems.observe(viewLifecycleOwner, observer)

        btnConfirm.setOnClickListener {
            val deliveryDate = if (prebookEnabled) {
                prebookDate?.toString() ?: run {
                    showToast("Select a delivery date for prebook")
                    return@setOnClickListener
                }
            } else null

            btnConfirm.isEnabled = false
            btnConfirm.text = "Processing..."
            cartViewModel.placeOrder(cartViewModel.cartItems.value.orEmpty(), deliveryDate) { success, message ->
                showToast(message)
                btnConfirm.isEnabled = true
                btnConfirm.text = "Confirm Order"
                if (success) dialog.dismiss()
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.setContentView(view)
        dialog.show()
        val behavior = BottomSheetBehavior.from(view.parent as View)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
        checkoutDialog = dialog
        dialog.setOnDismissListener {
            checkoutObserver?.let { cartViewModel.cartItems.removeObserver(it) }
            checkoutObserver = null
            checkoutAdapter = null
            checkoutDialog = null
        }
    }

    private fun observePendingOrders() {
        cartViewModel.pendingOrders.observe(viewLifecycleOwner) { orders ->
            val hasOrders = orders.isNotEmpty()
            rvPendingOrders.visibility = if (hasOrders) View.VISIBLE else View.GONE
            tvNoPendingOrders.visibility = if (hasOrders) View.GONE else View.VISIBLE
            pendingOrdersAdapter.submitList(orders)
        }

        cartViewModel.pendingLoading.observe(viewLifecycleOwner) { loading ->
            pbPendingOrders.visibility = if (loading) View.VISIBLE else View.GONE
        }

        cartViewModel.pendingError.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrBlank()) showToast(error)
        }
    }
}
