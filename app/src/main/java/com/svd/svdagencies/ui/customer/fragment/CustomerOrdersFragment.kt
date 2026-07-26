package com.svd.svdagencies.ui.customer.fragment

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
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
import com.svd.svdagencies.ui.customer.viewmodel.PlaceOrderViewModel
import com.svd.svdagencies.utils.CustomerOrderWindow
import com.svd.svdagencies.utils.LatestCustomerOrder
import com.svd.svdagencies.utils.LatestCustomerOrderItem
import com.svd.svdagencies.utils.LatestCustomerOrderStore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class CustomerOrdersFragment : Fragment(R.layout.customer_orders) {

    companion object {
        private const val ARG_EDIT_LATEST_ORDER = "edit_latest_order"

        fun newInstance(editLatestOrder: Boolean = false): CustomerOrdersFragment {
            return CustomerOrdersFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_EDIT_LATEST_ORDER, editLatestOrder)
                }
            }
        }
    }

    private lateinit var orderAdapter: OrderProductAdapter
    private lateinit var summaryAdapter: SummaryAdapter
    private val viewModel: PlaceOrderViewModel by viewModels()

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var chipGroup: ChipGroup
    private lateinit var cardOrderSummary: MaterialCardView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvOrderingWindow: TextView
    private lateinit var btnPlaceOrder: MaterialButton

    private var shouldLoadLatestOrder = false
    private var pendingServerOrder: LatestCustomerOrder? = null

    private val currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    private val gson = Gson()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        val rvOrders = view.findViewById<RecyclerView>(R.id.rvOrders)
        chipGroup = view.findViewById(R.id.chipGroup)
        val rvSummary = view.findViewById<RecyclerView>(R.id.rvSummary)
        tvOrderingWindow = view.findViewById(R.id.tvOrderingWindow)
        btnPlaceOrder = view.findViewById(R.id.btnPlaceOrder)
        cardOrderSummary = view.findViewById(R.id.cardOrderSummary)
        tvTotalAmount = view.findViewById(R.id.tvTotalAmount)
        shouldLoadLatestOrder = arguments?.getBoolean(ARG_EDIT_LATEST_ORDER, false) == true

        orderAdapter = OrderProductAdapter(
            products = viewModel.currentCategoryProducts,
            cartQuantities = viewModel.cartQuantities
        ) {
            updateSummary()
        }

        rvOrders.layoutManager = LinearLayoutManager(requireContext())
        rvOrders.adapter = orderAdapter

        summaryAdapter = SummaryAdapter(viewModel.cartQuantities, viewModel.allProducts)
        rvSummary.layoutManager = LinearLayoutManager(requireContext())
        rvSummary.adapter = summaryAdapter

        swipeRefresh.setOnRefreshListener {
            refreshOrdersScreen()
        }

        refreshOrdersScreen()

        btnPlaceOrder.setOnClickListener {
            if (!CustomerOrderWindow.isOpen()) {
                Toast.makeText(
                    requireContext(),
                    "Orders can be placed or edited only between 9:00 AM and 4:30 PM.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

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
        updateOrderWindowUi()
    }

    private fun refreshOrdersScreen() {
        loadCategories()
        fetchExistingOrderState()
    }

    private fun updateSummary() {
        summaryAdapter.notifyDataSetChanged()

        var total = 0.0
        viewModel.cartQuantities.forEach { (id, qty) ->
            val product = viewModel.allProducts[id] ?: return@forEach
            total += product.calculateTotal(qty)
        }

        tvTotalAmount.text = currency.format(total)
        cardOrderSummary.isVisible = viewModel.cartQuantities.isNotEmpty()
    }

    private fun placeOrder() {
        if (!CustomerOrderWindow.isOpen()) {
            Toast.makeText(
                requireContext(),
                "Orders can be placed or edited only between 9:00 AM and 4:30 PM.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        btnPlaceOrder.isEnabled = false

        val orderItems = viewModel.cartQuantities.map { (productId, quantity) ->
            OrderItemRequest(itemId = productId, quantity = quantity)
        }

        val submittedOrderItems = buildLatestOrderItems()
        val request = PlaceOrderRequest(items = orderItems)
        val api = ApiClient.retrofit.create(OrderApi::class.java)

        api.placeOrder(request).enqueue(object : Callback<PlaceOrderResponse> {
            override fun onResponse(
                call: Call<PlaceOrderResponse>,
                response: Response<PlaceOrderResponse>
            ) {
                val context = context ?: return
                btnPlaceOrder.isEnabled = true

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
                    Toast.makeText(
                        context,
                        response.body()?.message ?: "Order failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<PlaceOrderResponse>, t: Throwable) {
                val context = context ?: return
                btnPlaceOrder.isEnabled = CustomerOrderWindow.isOpen()
                Toast.makeText(context, t.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchExistingOrderState() {
        val api = ApiClient.retrofit.create(OrderApi::class.java)
        api.getCurrentDayOrders().enqueue(object : Callback<CurrentDayOrdersResponse> {
            override fun onResponse(
                call: Call<CurrentDayOrdersResponse>,
                response: Response<CurrentDayOrdersResponse>
            ) {
                if (context == null) return

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

                if (viewModel.allProducts.isNotEmpty()) {
                    bindExistingServerOrderIfNeeded()
                }
            }

            override fun onFailure(call: Call<CurrentDayOrdersResponse>, t: Throwable) {
                if (context == null) return
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

                    if (index == 0) {
                        chip.isChecked = true
                        loadProducts(category.id)
                    }
                }

                if (shouldLoadLatestOrder) {
                    restoreLatestOrderIntoCart()
                    shouldLoadLatestOrder = false
                    arguments?.putBoolean(ARG_EDIT_LATEST_ORDER, false)
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                val context = context ?: return
                swipeRefresh.isRefreshing = false
                Toast.makeText(context, "Failed to load categories", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadProducts(categoryId: Int) {
        swipeRefresh.isRefreshing = true

        val api = ApiClient.retrofit.create(ProductApi::class.java)
        api.getProducts(categoryId).enqueue(object : Callback<JsonElement> {
            override fun onResponse(
                call: Call<JsonElement>,
                response: Response<JsonElement>
            ) {
                if (context == null) return

                swipeRefresh.isRefreshing = false
                val list = parseProducts(response.body())

                viewModel.currentCategoryProducts.clear()
                viewModel.currentCategoryProducts.addAll(list)
                list.forEach { viewModel.allProducts[it.id] = it }

                orderAdapter.updateProducts(list)
                bindExistingServerOrderIfNeeded()
                updateSummary()
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                if (context == null) return
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
        return runCatching { gson.fromJson<List<ProductResponse>>(json, type) }.getOrDefault(emptyList())
    }

    private fun updateOrderWindowUi() {
        val isOpen = CustomerOrderWindow.isOpen()
        tvOrderingWindow.text = CustomerOrderWindow.statusMessage()
        btnPlaceOrder.isEnabled = isOpen
        btnPlaceOrder.text = if (isOpen) "Place Order" else "Ordering Closed"
        btnPlaceOrder.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                requireContext(),
                if (isOpen) R.color.icon_green else R.color.chip_unselected_border
            )
        )
    }

    private fun restoreLatestOrderIntoCart() {
        val context = context ?: return
        val latestOrder = LatestCustomerOrderStore.get(context) ?: return

        if (!CustomerOrderWindow.isOpen()) {
            Toast.makeText(
                context,
                "Latest order can be edited only between 9:00 AM and 4:30 PM.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        bindOrderItemsToCart(latestOrder.items)

        Toast.makeText(
            context,
            "Latest order loaded. You can update quantities now.",
            Toast.LENGTH_SHORT
        ).show()

        updateSummary()
    }

    private fun bindExistingServerOrderIfNeeded() {
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
        summaryAdapter.notifyDataSetChanged()
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
        summaryAdapter.notifyDataSetChanged()
        updateSummary()
    }
}
