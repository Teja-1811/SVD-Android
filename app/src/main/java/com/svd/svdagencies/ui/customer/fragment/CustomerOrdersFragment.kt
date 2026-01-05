package com.svd.svdagencies.ui.customer.fragment

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
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
import com.svd.svdagencies.data.model.customer.OrderItemRequest
import com.svd.svdagencies.data.model.customer.PlaceOrderRequest
import com.svd.svdagencies.data.model.customer.PlaceOrderResponse
import com.svd.svdagencies.data.model.customer.ProductResponse
import com.svd.svdagencies.ui.customer.adapter.OrderProductAdapter
import com.svd.svdagencies.ui.customer.adapter.SummaryAdapter
import com.svd.svdagencies.ui.customer.viewmodel.PlaceOrderViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.*

class CustomerOrdersFragment : Fragment(R.layout.fragment_customer_orders) {

    private lateinit var orderAdapter: OrderProductAdapter
    private lateinit var summaryAdapter: SummaryAdapter
    private val viewModel: PlaceOrderViewModel by viewModels()

    // Views
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var chipGroup: ChipGroup
    private lateinit var cardOrderSummary: MaterialCardView
    private lateinit var tvTotalAmount: TextView
    private lateinit var btnPlaceOrder: MaterialButton

    private val currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        val rvOrders = view.findViewById<RecyclerView>(R.id.rvOrders)
        chipGroup = view.findViewById(R.id.chipGroup)
        val rvSummary = view.findViewById<RecyclerView>(R.id.rvSummary)
        btnPlaceOrder = view.findViewById(R.id.btnPlaceOrder)
        cardOrderSummary = view.findViewById(R.id.cardOrderSummary)
        tvTotalAmount = view.findViewById(R.id.tvTotalAmount)

        // ---------- ORDER LIST ----------
        orderAdapter = OrderProductAdapter(
            products = viewModel.currentCategoryProducts,
            cartQuantities = viewModel.cartQuantities
        ) {
            updateSummary()
        }

        rvOrders.layoutManager = LinearLayoutManager(requireContext())
        rvOrders.adapter = orderAdapter

        // ---------- SUMMARY LIST ----------
        summaryAdapter = SummaryAdapter(viewModel.cartQuantities, viewModel.allProducts)
        rvSummary.layoutManager = LinearLayoutManager(requireContext())
        rvSummary.adapter = summaryAdapter

        // ---------- REFRESH LISTENER ----------
        swipeRefresh.setOnRefreshListener {
            loadCategories()
        }

        // ---------- LOAD DATA ----------
        loadCategories()

        // ---------- PLACE ORDER ----------
        btnPlaceOrder.setOnClickListener {
            if (viewModel.cartQuantities.isEmpty()) {
                Toast.makeText(requireContext(), "Select items", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            placeOrder()
        }

        updateSummary()
    }

    // ================= UPDATE SUMMARY =================
    private fun updateSummary() {

        summaryAdapter.notifyDataSetChanged()

        var total = 0.0

        viewModel.cartQuantities.forEach { (id, qty) ->

            val product = viewModel.allProducts[id] ?: return@forEach

            val pcsCount = product.pcs_count ?: 1

            val totalPieces = qty * pcsCount

            total += totalPieces * product.selling_price
        }

        tvTotalAmount.text = currency.format(total)

        cardOrderSummary.isVisible = viewModel.cartQuantities.isNotEmpty()
    }

    // ================= PLACE ORDER =================
    private fun placeOrder() {

        btnPlaceOrder.isEnabled = false

        val orderItems = viewModel.cartQuantities.map { (productId, quantity) ->
            OrderItemRequest(itemId = productId, quantity = quantity)
        }

        val request = PlaceOrderRequest(items = orderItems)

        val api = ApiClient.retrofit.create(OrderApi::class.java)

        api.placeOrder(request).enqueue(object : Callback<PlaceOrderResponse> {

            override fun onResponse(
                call: Call<PlaceOrderResponse>,
                response: Response<PlaceOrderResponse>
            ) {
                btnPlaceOrder.isEnabled = true

                if (response.isSuccessful && response.body()?.success == true) {

                    Toast.makeText(
                        requireContext(),
                        "Order placed with ID: ${response.body()?.orderNumber}",
                        Toast.LENGTH_LONG
                    ).show()

                    viewModel.cartQuantities.clear()

                    orderAdapter.notifyDataSetChanged()
                    summaryAdapter.notifyDataSetChanged()

                    updateSummary()

                } else {
                    Toast.makeText(
                        requireContext(),
                        response.body()?.message ?: "Order failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<PlaceOrderResponse>, t: Throwable) {
                btnPlaceOrder.isEnabled = true
                Toast.makeText(requireContext(), t.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ================= LOAD CATEGORIES =================
    private fun loadCategories() {

        swipeRefresh.isRefreshing = true

        val api = ApiClient.retrofit.create(ProductApi::class.java)

        api.getCategories().enqueue(object : Callback<List<CategoryResponse>> {

            override fun onResponse(
                call: Call<List<CategoryResponse>>,
                response: Response<List<CategoryResponse>>
            ) {
                val categories = response.body()

                if (categories.isNullOrEmpty()) {
                    swipeRefresh.isRefreshing = false
                    return
                }

                chipGroup.removeAllViews()

                categories.forEachIndexed { index, category ->

                    val chip = Chip(requireContext()).apply {
                        text = category.name
                        isCheckable = true
                        setOnClickListener { loadProducts(category.id) }
                    }

                    chipGroup.addView(chip)

                    if (index == 0) {
                        chip.isChecked = true
                        loadProducts(category.id)
                    }
                }
            }

            override fun onFailure(call: Call<List<CategoryResponse>>, t: Throwable) {
                swipeRefresh.isRefreshing = false
                Toast.makeText(requireContext(), "Failed to load categories", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ================= LOAD PRODUCTS =================
    private fun loadProducts(categoryId: Int) {

        swipeRefresh.isRefreshing = true

        val api = ApiClient.retrofit.create(ProductApi::class.java)

        api.getProducts(categoryId)
            .enqueue(object : Callback<List<ProductResponse>> {

                override fun onResponse(
                    call: Call<List<ProductResponse>>,
                    response: Response<List<ProductResponse>>
                ) {
                    swipeRefresh.isRefreshing = false

                    val list = response.body() ?: return

                    viewModel.currentCategoryProducts.clear()
                    viewModel.currentCategoryProducts.addAll(list)

                    list.forEach { viewModel.allProducts[it.id] = it }

                    orderAdapter.updateProducts(list)

                    updateSummary()
                }

                override fun onFailure(
                    call: Call<List<ProductResponse>>,
                    t: Throwable
                ) {
                    swipeRefresh.isRefreshing = false
                }
            })
    }
}
