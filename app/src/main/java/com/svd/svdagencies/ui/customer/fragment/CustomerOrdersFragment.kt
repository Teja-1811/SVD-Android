package com.svd.svdagencies.ui.customer.fragment

import com.svd.svdagencies.utils.SharedDatePickerDialog as DatePickerDialog
import android.os.Bundle
import com.svd.svdagencies.utils.DeliveryCoordinates
import com.svd.svdagencies.utils.DeliveryLocationPicker
import com.svd.svdagencies.utils.showLoading
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.api.customer.OrderApi
import com.svd.svdagencies.data.api.customer.ProductApi
import com.svd.svdagencies.data.model.customer.CategoryResponse
import com.svd.svdagencies.data.model.customer.CurrentDayOrdersResponse
import com.svd.svdagencies.data.model.customer.CustomerOrderPayload
import com.svd.svdagencies.data.model.customer.OrderItemRequest
import com.svd.svdagencies.data.model.customer.PlaceOrderRequest
import com.svd.svdagencies.data.model.customer.PlacedOrderItem
import com.svd.svdagencies.data.model.customer.PlaceOrderResponse
import com.svd.svdagencies.data.model.customer.ProductResponse
import com.svd.svdagencies.ui.customer.adapter.OrderProductAdapter
import com.svd.svdagencies.ui.customer.adapter.SummaryAdapter
import com.svd.svdagencies.ui.customer.PortalMainActivity
import com.svd.svdagencies.ui.customer.viewmodel.PlaceOrderViewModel
import com.svd.svdagencies.utils.SessionManager
import com.svd.svdagencies.utils.UserRole
import com.svd.svdagencies.utils.LatestCustomerOrder
import com.svd.svdagencies.utils.LatestCustomerOrderItem
import com.svd.svdagencies.utils.LatestCustomerOrderStore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale
import java.util.Calendar
import java.text.SimpleDateFormat

class CustomerOrdersFragment : Fragment() {

    companion object {
        private const val ARG_EDIT_LATEST_ORDER = "edit_latest_order"

        fun newInstance(editLatestOrder: Boolean = false, productId: Int? = null): CustomerOrdersFragment {
            return CustomerOrdersFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_EDIT_LATEST_ORDER, editLatestOrder)
                    productId?.let { putInt("initial_product_id", it) }
                }
            }
        }
    }

    private val deliveryLocation = DeliveryLocationPicker(this) { requireContext() }
    private lateinit var orderAdapter: OrderProductAdapter
    private lateinit var summaryAdapter: SummaryAdapter
    private val viewModel: PlaceOrderViewModel by activityViewModels()

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var chipGroup: ChipGroup
    private lateinit var cardOrderSummary: MaterialCardView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvOrderingWindow: TextView
    private lateinit var btnPlaceOrder: MaterialButton
    private var orderTypeToggle: MaterialButtonToggleGroup? = null
    private var btnPreOrderDate: MaterialButton? = null
    private val preOrderDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    private var selectedUserOrderType = "regular"
    private var regularOrdersOpen: Boolean? = null
    private var regularOrderMessage: String = ""

    private var placingOrder = false
    private var choosingLocation = false
    private var orderCall: Call<PlaceOrderResponse>? = null
    private var shouldLoadLatestOrder = false
    private var pendingServerOrder: LatestCustomerOrder? = null

    private val currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    private val gson = Gson()

    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(if (SessionManager(requireContext()).getRole() == com.svd.svdagencies.utils.UserRole.USER) R.layout.user_orders else R.layout.customer_orders, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        val rvOrders = view.findViewById<RecyclerView>(R.id.rvOrders)
        chipGroup = view.findViewById(R.id.chipGroup)
        val rvSummary = view.findViewById<RecyclerView>(R.id.rvSummary)
        tvOrderingWindow = view.findViewById(R.id.tvOrderingWindow)
        btnPlaceOrder = view.findViewById(R.id.btnPlaceOrder)
        orderTypeToggle = view.findViewById(R.id.orderTypeToggle)
        btnPreOrderDate = requireActivity().findViewById(R.id.btnPreOrderDate)
        cardOrderSummary = view.findViewById(R.id.cardOrderSummary)
        tvTotalAmount = view.findViewById(R.id.tvTotalAmount)
        shouldLoadLatestOrder = arguments?.getBoolean(ARG_EDIT_LATEST_ORDER, false) == true

        orderAdapter = OrderProductAdapter(
            products = viewModel.currentCategoryProducts.toMutableList(),
            cartQuantities = viewModel.cartQuantities
        ) {
            updateSummary()
        }

        rvOrders.layoutManager = LinearLayoutManager(requireContext())
        rvOrders.adapter = orderAdapter

        summaryAdapter = SummaryAdapter(viewModel.cartQuantities, viewModel.allProducts)
        rvSummary.layoutManager = LinearLayoutManager(requireContext())
        rvSummary.adapter = summaryAdapter

        btnPreOrderDate?.setOnClickListener {
            if (!isUserPortal() || selectedUserOrderType == "pre_order") choosePreOrderDate()
        }
        (activity as? PortalMainActivity)?.setOrderDatePickerVisible(true)
        if (isUserPortal()) {
            orderTypeToggle?.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (!isChecked) return@addOnButtonCheckedListener
                selectedUserOrderType = when (checkedId) {
                    R.id.btnInstantOrder -> "instant"
                    R.id.btnPreOrder -> "pre_order"
                    else -> "regular"
                }
                applyUserOrderType()
            }
            orderTypeToggle?.check(R.id.btnRegularOrder)
            applyUserOrderType()
        } else {
            viewModel.isPreOrder = true
            tvOrderingWindow.isVisible = false
            updatePreOrderDateLabel()
        }


        swipeRefresh.setOnRefreshListener {
            refreshOrdersScreen()
        }

        refreshOrdersScreen()

        btnPlaceOrder.setOnClickListener {
            if (viewModel.cartQuantities.isEmpty()) {
                Toast.makeText(requireContext(), "Select items", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            placeOrder()
        }

        updateOrderWindowUi()
        updateSummary()
    }

    override fun onResume() {
        super.onResume()
        refreshOrderTimings()
        updateOrderWindowUi()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        (activity as? PortalMainActivity)?.setOrderDatePickerVisible(!hidden)
        if (!hidden && view != null && ::orderAdapter.isInitialized) {
            orderAdapter.notifyDataSetChanged()
            updateSummary()
            refreshOrderTimings()
        }
    }

    private fun refreshOrderTimings() {
        ApiClient.customerApi.getOrderTimings().enqueue(object : Callback<com.google.gson.JsonObject> {
            override fun onResponse(call: Call<com.google.gson.JsonObject>, response: Response<com.google.gson.JsonObject>) {
                if (!isAdded || view == null || !response.isSuccessful) return
                val regular = response.body()?.getAsJsonObject("regular") ?: return
                regularOrdersOpen = regular.get("is_open")?.asBoolean
                regularOrderMessage = regular.get("message")?.asString.orEmpty()
                updateOrderWindowUi()
            }

            override fun onFailure(call: Call<com.google.gson.JsonObject>, t: Throwable) {
                if (isAdded && view != null) updateOrderWindowUi()
            }
        })
    }

    private fun refreshOrdersScreen() {
        refreshOrderTimings()
        loadCategories()
        if (shouldLoadLatestOrder) fetchExistingOrderState()
    }

    private fun updateSummary() {
        summaryAdapter.refresh()

        var total = 0.0
        viewModel.cartQuantities.forEach { (id, qty) ->
            val product = viewModel.allProducts[id] ?: return@forEach
            total += product.calculateTotal(qty)
        }

        val isUser = isUserPortal()
        val deliveryCharge = if (isUser && selectedUserOrderType == "instant" && viewModel.cartQuantities.isNotEmpty()) 20.0 else 0.0
        view?.findViewById<TextView>(R.id.tvOrderDeliveryCharge)?.apply {
            isVisible = isUser
            text = "Delivery charge: ${currency.format(deliveryCharge)}"
        }
        tvTotalAmount.text = currency.format(total + deliveryCharge)
        cardOrderSummary.isVisible = viewModel.cartQuantities.isNotEmpty()
    }

    private fun placeOrder(location: DeliveryCoordinates? = null) {
        if (placingOrder || (choosingLocation && location == null)) return
        if (SessionManager(requireContext()).getRole() == UserRole.USER && location == null) {
            choosingLocation = true
            btnPlaceOrder.showLoading(true, "Choose location…")
            deliveryLocation.choose(onDismissed = {
                choosingLocation = false
                if (view != null && !placingOrder) { btnPlaceOrder.showLoading(false); updateOrderWindowUi() }
            }) {
                choosingLocation = false
                btnPlaceOrder.showLoading(false)
                placeOrder(it)
            }
            return
        }
        placingOrder = true
        btnPlaceOrder.showLoading(true, "Placing order…")

        val orderItems = viewModel.cartQuantities.map { (productId, quantity) ->
            OrderItemRequest(itemId = productId, quantity = quantity)
        }

        val submittedOrderItems = buildLatestOrderItems()
        val deliveryDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(preOrderDate.time)
        val request = PlaceOrderRequest(
            items = orderItems,
            orderType = if (isUserPortal()) selectedUserOrderType else "pre_order",
            deliveryDate = deliveryDate,
            deliveryLatitude = location?.latitude,
            deliveryLongitude = location?.longitude,
        )
        val api = ApiClient.retrofit.create(OrderApi::class.java)

        val screen = view ?: return
        orderCall = api.placeOrder(request)
        orderCall?.enqueue(object : Callback<PlaceOrderResponse> {
            override fun onResponse(
                call: Call<PlaceOrderResponse>,
                response: Response<PlaceOrderResponse>
            ) {
                if (!isAdded || view !== screen || call.isCanceled) return
                val context = context ?: return
                placingOrder = false
                btnPlaceOrder.showLoading(false)
                updateOrderWindowUi()

                if (response.isSuccessful && response.body()?.success == true) {
                    val responseBody = response.body()
                    val boundOrderItems = responseBody?.items
                        ?.takeIf { it.isNotEmpty() }
                        ?.let(::mapResponseItemsToLatestOrderItems)
                        ?: submittedOrderItems

                    Toast.makeText(
                        context,
                        "Order placed with ID: ${responseBody?.orderNumber}",
                        Toast.LENGTH_LONG
                    ).show()

                    LatestCustomerOrderStore.save(
                        context,
                        LatestCustomerOrder(
                            orderNumber = responseBody?.orderNumber,
                            placedAtMillis = System.currentTimeMillis(),
                            items = boundOrderItems
                        )
                    )

                    bindOrderItemsToCart(boundOrderItems)
                    updateOrderWindowUi()
                    updateSummary()

                } else {
                    val errorBody = runCatching { com.google.gson.JsonParser.parseString(response.errorBody()?.string()).asJsonObject }.getOrNull()
                    if (errorBody?.get("code")?.asString == "insufficient_wallet") {
                        com.svd.svdagencies.ui.customer.WalletTopUpDialog.show(this@CustomerOrdersFragment,
                            errorBody.get("required_amount").asDouble, errorBody.get("top_up_amount").asDouble)
                        return
                    }
                    refreshOrderTimings()
                    Toast.makeText(
                        context,
                        response.body()?.message ?: errorBody?.get("message")?.asString ?: "Order failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<PlaceOrderResponse>, t: Throwable) {
                if (!isAdded || view == null) return
                val context = context ?: return
                if (view !== screen || call.isCanceled) return
                placingOrder = false
                btnPlaceOrder.showLoading(false)
                updateOrderWindowUi()
                Toast.makeText(context, t.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun choosePreOrderDate() {
        val picker = DatePickerDialog(requireContext(), { _, year, month, day ->
            preOrderDate.set(year, month, day)
            updatePreOrderDateLabel()
            updateOrderWindowUi()
        }, preOrderDate.get(Calendar.YEAR), preOrderDate.get(Calendar.MONTH), preOrderDate.get(Calendar.DAY_OF_MONTH))
        picker.datePicker.minDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, if (isUserPortal()) 2 else 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        picker.show()
    }

    private fun updatePreOrderDateLabel() {
        btnPreOrderDate?.text = SimpleDateFormat("dd MMM yyyy", Locale.US).format(preOrderDate.time)
    }

    private fun isUserPortal() = SessionManager(requireContext()).getRole() == UserRole.USER

    private fun applyUserOrderType() {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        when (selectedUserOrderType) {
            "instant" -> preOrderDate.timeInMillis = today.timeInMillis
            "regular" -> {
                today.add(Calendar.DAY_OF_YEAR, 1)
                preOrderDate.timeInMillis = today.timeInMillis
            }
            "pre_order" -> {
                val earliestPreOrderDate = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 2) }
                if (preOrderDate.before(earliestPreOrderDate)) {
                    preOrderDate.timeInMillis = earliestPreOrderDate.timeInMillis
                }
            }
        }
        viewModel.isPreOrder = selectedUserOrderType == "pre_order"
        btnPreOrderDate?.isEnabled = selectedUserOrderType == "pre_order"
        btnPreOrderDate?.alpha = if (selectedUserOrderType == "pre_order") 1f else 0.85f
        updatePreOrderDateLabel()
        updateOrderWindowUi()
        updateSummary()
    }

    private fun fetchExistingOrderState() {
        val api = ApiClient.retrofit.create(OrderApi::class.java)
        val selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(preOrderDate.time)
        api.getCurrentDayOrders(selectedDate).enqueue(object : Callback<CurrentDayOrdersResponse> {
            override fun onResponse(
                call: Call<CurrentDayOrdersResponse>,
                response: Response<CurrentDayOrdersResponse>
            ) {
                if (!isAdded || view == null) return

                if (response.code() == 404) {
                    clearPendingOrderState()
                    return
                }

                val payload = response.body() ?: run {
                    clearPendingOrderState()
                    return
                }

                val existingOrder = payload.orders.firstOrNull {
                    !it.items.isNullOrEmpty() && it.status !in setOf("cancelled", "rejected", "delivered")
                }

                if (existingOrder == null) {
                    clearPendingOrderState()
                    return
                }

                val latestOrder = buildLatestOrderFromPayload(existingOrder)
                pendingServerOrder = latestOrder
                LatestCustomerOrderStore.save(requireContext(), latestOrder)

                if (viewModel.allProducts.isNotEmpty() && shouldLoadLatestOrder) {
                    bindExistingServerOrderIfNeeded()
                }
            }

            override fun onFailure(call: Call<CurrentDayOrdersResponse>, t: Throwable) {
                if (!isAdded || view == null) return
                swipeRefresh.isRefreshing = false
            }
        })
    }

    private fun loadCategories() {
        swipeRefresh.isRefreshing = true

        val api = ApiClient.retrofit.create(ProductApi::class.java)
        api.getCategories().enqueue(object : Callback<JsonElement> {
            override fun onResponse(
                call: Call<JsonElement>,
                response: Response<JsonElement>
            ) {
                if (!isAdded || view == null) return
                val context = context ?: return
                val categories = parseCategories(response.body())

                if (categories.isNullOrEmpty()) {
                    swipeRefresh.isRefreshing = false
                    return
                }

                chipGroup.removeAllViews()

                categories.forEachIndexed { index, category ->
                    val chip = Chip(context).apply {
                        text = category.name
                        tag = category.id
                        id = View.generateViewId()
                        isCheckable = true
                        chipBackgroundColor =
                            ContextCompat.getColorStateList(context, R.color.chip_background_color)
                        setTextColor(
                            ContextCompat.getColorStateList(context, R.color.chip_text_color)
                        )
                        chipStrokeColor =
                            ContextCompat.getColorStateList(context, R.color.chip_unselected_border)
                        chipStrokeWidth = 1f
                        setOnClickListener { loadProducts(category.id) }
                    }

                    chipGroup.addView(chip)

                    if (index == 0 && arguments?.containsKey("initial_product_id") != true) {
                        chip.isChecked = true
                        loadProducts(category.id)
                    }
                }

                if (arguments?.containsKey("initial_product_id") == true) loadProducts(null)

                if (shouldLoadLatestOrder) {
                    restoreLatestOrderIntoCart()
                    shouldLoadLatestOrder = false
                    arguments?.putBoolean(ARG_EDIT_LATEST_ORDER, false)
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                if (!isAdded || view == null) return
                val context = context ?: return
                swipeRefresh.isRefreshing = false
                Toast.makeText(context, "Failed to load categories", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadProducts(categoryId: Int?) {
        swipeRefresh.isRefreshing = true

        val api = ApiClient.retrofit.create(ProductApi::class.java)
        api.getProducts(categoryId).enqueue(object : Callback<JsonElement> {
            override fun onResponse(
                call: Call<JsonElement>,
                response: Response<JsonElement>
            ) {
                if (!isAdded || view == null) return

                swipeRefresh.isRefreshing = false
                val all = parseProducts(response.body())
                val list = all
                val initialId = arguments?.getInt("initial_product_id", -1) ?: -1
                if (list.any { it.id == initialId }) {
                    viewModel.cartQuantities[initialId] = 1.0
                    arguments?.remove("initial_product_id")
                }

                viewModel.currentCategoryProducts.clear()
                viewModel.currentCategoryProducts.addAll(list)
                list.forEach { viewModel.allProducts[it.id] = it }

                orderAdapter.updateProducts(list)
                bindExistingServerOrderIfNeeded()
                updateSummary()
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                if (!isAdded || view == null) return
                swipeRefresh.isRefreshing = false
            }
        })
    }

    private fun parseCategories(body: JsonElement?): List<CategoryResponse> {
        if (body == null || body.isJsonNull) return emptyList()
        val json = if (body.isJsonObject && body.asJsonObject.has("categories")) {
            body.asJsonObject.get("categories")
        } else {
            body
        }
        val type = object : TypeToken<List<CategoryResponse>>() {}.type
        return runCatching { gson.fromJson<List<CategoryResponse>>(json, type) }.getOrDefault(emptyList())
    }

    private fun parseProducts(body: JsonElement?): List<ProductResponse> {
        if (body == null || body.isJsonNull) return emptyList()
        val json = if (body.isJsonObject && body.asJsonObject.has("products")) {
            body.asJsonObject.get("products")
        } else {
            body
        }
        val type = object : TypeToken<List<ProductResponse>>() {}.type
        val products = runCatching { gson.fromJson<List<ProductResponse>>(json, type) }.getOrDefault(emptyList())
        return if (SessionManager(requireContext()).getRole() == UserRole.USER) {
            products.map { it.copy(selling_price = it.mrp, margin = 0.0) }
        } else products
    }

    override fun onDestroyView() {
        orderCall?.cancel()
        placingOrder = false
        choosingLocation = false
        if (::btnPlaceOrder.isInitialized) btnPlaceOrder.showLoading(false)
        super.onDestroyView()
    }

    private fun updateOrderWindowUi() {
        if (placingOrder || choosingLocation) return
        val requiresAdminWindow = if (isUserPortal()) {
            selectedUserOrderType == "regular"
        } else {
            val tomorrow = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            !preOrderDate.after(tomorrow)
        }
        val closedForTomorrow = requiresAdminWindow && regularOrdersOpen == false
        btnPlaceOrder.isEnabled = !closedForTomorrow
        btnPlaceOrder.text = if (closedForTomorrow) "Orders closed" else "Place Order"
        tvOrderingWindow.isVisible = requiresAdminWindow && regularOrderMessage.isNotBlank()
        if (tvOrderingWindow.isVisible) tvOrderingWindow.text = regularOrderMessage
    }

    private fun restoreLatestOrderIntoCart() {
        if (!isAdded || view == null) return
        val context = context ?: return
        val latestOrder = LatestCustomerOrderStore.get(context) ?: return

        bindOrderItemsToCart(latestOrder.items)

        Toast.makeText(
            context,
            "Latest order loaded. You can update quantities now.",
            Toast.LENGTH_SHORT
        ).show()

        updateSummary()
    }

    private fun bindExistingServerOrderIfNeeded() {
        if (arguments?.getBoolean(ARG_EDIT_LATEST_ORDER, false) != true) return
        val serverOrder = pendingServerOrder ?: return
        pendingServerOrder = null

        bindOrderItemsToCart(serverOrder.items)
        updateSummary()
    }

    private fun buildLatestOrderItems(): List<LatestCustomerOrderItem> {
        return viewModel.cartQuantities.mapNotNull { (productId, quantity) ->
            val product = viewModel.allProducts[productId] ?: return@mapNotNull null
            LatestCustomerOrderItem(
                productId = productId,
                name = product.name,
                company = product.company,
                quantity = quantity,
                unitPrice = product.selling_price
            )
        }
    }

    private fun mapResponseItemsToLatestOrderItems(items: List<PlacedOrderItem>): List<LatestCustomerOrderItem> {
        return items.mapNotNull { item ->
            val product = viewModel.allProducts[item.itemId]
            val fallbackName = product?.name ?: item.name
            val fallbackCompany = product?.company ?: item.company ?: ""
            val price = item.unitPrice ?: product?.selling_price

            if (fallbackName.isNullOrBlank() || price == null) {
                return@mapNotNull null
            }

            LatestCustomerOrderItem(
                productId = item.itemId,
                name = fallbackName,
                company = fallbackCompany,
                quantity = item.quantity,
                unitPrice = price
            )
        }
    }

    private fun bindOrderItemsToCart(items: List<LatestCustomerOrderItem>) {
        viewModel.cartQuantities.clear()
        items.forEach { item ->
            viewModel.cartQuantities[item.productId] = item.quantity
            if (!viewModel.allProducts.containsKey(item.productId)) {
                viewModel.allProducts[item.productId] = ProductResponse(
                    id = item.productId,
                    name = item.name,
                    company = item.company,
                    mrp = item.unitPrice,
                    selling_price = item.unitPrice,
                    margin = 0.0,
                    stock = 0,
                    image = "",
                    pcs_count = 1
                )
            }
        }

        orderAdapter.notifyDataSetChanged()
        summaryAdapter.refresh()
    }

    private fun buildLatestOrderFromPayload(order: CustomerOrderPayload): LatestCustomerOrder {
        val mappedItems = mapResponseItemsToLatestOrderItems(order.items ?: emptyList())
        return LatestCustomerOrder(
            orderNumber = order.orderNumber,
            placedAtMillis = System.currentTimeMillis(),
            items = mappedItems
        )
    }

    private fun clearPendingOrderState() {
        val context = context
        pendingServerOrder = null
        viewModel.cartQuantities.clear()
        context?.let { LatestCustomerOrderStore.clear(it) }
        swipeRefresh.isRefreshing = false
        orderAdapter.notifyDataSetChanged()
        summaryAdapter.refresh()
        updateSummary()
    }
}
