package com.svd.svdagencies.ui.admin.customer

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.admin.CustomerItemDiscountApi
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.customerData.CustomerDiscountCustomer
import com.svd.svdagencies.data.model.admin.customerData.CustomerItemDiscountValue
import com.svd.svdagencies.data.model.admin.customerData.SaveCustomerItemDiscountsRequest
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomerItemDiscountActivity : AdminBaseActivity() {
    private val api by lazy { ApiClient.retrofit.create(CustomerItemDiscountApi::class.java) }
    private lateinit var customerSpinner: Spinner
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var adapter: CustomerItemDiscountAdapter
    private lateinit var saveButton: MaterialButton
    private var customers = emptyList<CustomerDiscountCustomer>()
    private var selectedCustomerId: Int? = null
    private var loadingCustomers = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_customer_item_discounts)
        setupAdminLayout("Customer Discounts")

        customerSpinner = findViewById(R.id.spinnerCustomer)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        saveButton = findViewById(R.id.btnSaveDiscounts)
        adapter = CustomerItemDiscountAdapter()
        findViewById<RecyclerView>(R.id.rvDiscountItems).apply {
            layoutManager = LinearLayoutManager(this@CustomerItemDiscountActivity)
            adapter = this@CustomerItemDiscountActivity.adapter
        }

        customerSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!loadingCustomers && position > 0) {
                    val customer = customers[position - 1]
                    selectedCustomerId = customer.id
                    adapter.setUsesMrp(customer.user_type.equals("user", ignoreCase = true))
                    loadDashboard(selectedCustomerId)
                }
            }
        }
        swipeRefresh.setOnRefreshListener { loadDashboard(selectedCustomerId) }
        saveButton.setOnClickListener { saveDiscounts() }
        loadDashboard(null)
    }

    private fun loadDashboard(customerId: Int?) {
        swipeRefresh.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.getDashboard(customerId)
                withContext(Dispatchers.Main) {
                    if (customerId == null) {
                        loadingCustomers = true
                        customers = response.customers
                        val labels = listOf("Select customer") + customers.map {
                            listOf(it.name, it.shop_name).filter(String::isNotBlank).joinToString(" — ")
                        }
                        customerSpinner.adapter = ArrayAdapter(this@CustomerItemDiscountActivity, android.R.layout.simple_spinner_dropdown_item, labels)
                        loadingCustomers = false
                    }
                    adapter.submitList(response.items)
                    saveButton.isEnabled = selectedCustomerId != null
                    swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    Toast.makeText(this@CustomerItemDiscountActivity, "Unable to load discounts", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveDiscounts() {
        val customerId = selectedCustomerId ?: return
        saveButton.isEnabled = false
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = SaveCustomerItemDiscountsRequest(
                    customerId,
                    adapter.values().map { CustomerItemDiscountValue(it.id, it.discount_per_unit) }
                )
                val response = api.saveDiscounts(request)
                withContext(Dispatchers.Main) {
                    saveButton.isEnabled = true
                    Toast.makeText(this@CustomerItemDiscountActivity, response.message ?: "Discounts saved", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    saveButton.isEnabled = true
                    Toast.makeText(this@CustomerItemDiscountActivity, "Unable to save discounts", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
