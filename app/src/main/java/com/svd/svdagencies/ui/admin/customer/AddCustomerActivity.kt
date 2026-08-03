package com.svd.svdagencies.ui.admin.customer

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.svd.svdagencies.data.api.admin.CustomerDashboardApi
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.customerData.AddCustomerRequest
import com.svd.svdagencies.data.model.admin.customerData.CustomerItem
import com.svd.svdagencies.data.model.delivery.DeliveryRoute
import com.svd.svdagencies.databinding.AdminCustomerAddBinding
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import com.svd.svdagencies.utils.NetworkMessageUtils
import com.svd.svdagencies.utils.showLoading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse

class AddCustomerActivity : AdminBaseActivity() {

    private lateinit var binding: AdminCustomerAddBinding
    private var customerToUpdate: CustomerItem? = null
    private lateinit var api: CustomerDashboardApi
    private var routes: List<DeliveryRoute> = emptyList()
    private var selectedRouteId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdminCustomerAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        api = ApiClient.adminCustomerDashboard

        // Check if we are in Update mode
        customerToUpdate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("CUSTOMER_TO_UPDATE", CustomerItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("CUSTOMER_TO_UPDATE")
        }

        if (customerToUpdate != null) {
            setupAdminLayout("Update Customer")
            binding.btnAddCustomer.text = "Update Customer"
            populateFields(customerToUpdate!!)
        } else {
            setupAdminLayout("Add Customer")
            binding.btnAddCustomer.text = "Add Customer"
        }

        loadRoutes()
        setupListeners()
    }

    private fun populateFields(customer: CustomerItem) {
        binding.etCustomerName.setText(customer.name)
        binding.etShopName.setText(customer.shop_name)
        binding.etPhone.setText(customer.phone)
        selectedRouteId = customer.route_id
        binding.actRoute.setText(customer.route_name.orEmpty(), false)
        
        if (customer.id != null && customer.id != 0) {
             fetchFullDetailsAndPopulate(customer.id)
        }
    }

    private fun fetchFullDetailsAndPopulate(id: Int) {
        lifecycleScope.launch {
            try {
                val detail = withContext(Dispatchers.IO) { api.getCustomerDetail(id) }
                binding.etCity.setText(detail.city)
                binding.etState.setText(detail.state)
                binding.etRetailerId.setText(detail.retailer_id)
                binding.etArea.setText(detail.area)
                selectedRouteId = detail.route_id
                binding.actRoute.setText(detail.route_name.orEmpty(), false)
                binding.etPinCode.setText(detail.pincode)
                binding.etAddressLine1.setText(detail.address)
            } catch (e: Exception) {
                // Ignore failure
            }
        }
    }

    private fun setupListeners() {
        binding.btnAddCustomer.setOnClickListener {
            saveCustomer()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun loadRoutes() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { ApiClient.deliveryApi.getRoutes().awaitResponse() }
                routes = response.body().orEmpty()
                val routeNames = listOf("No Route") + routes.map { it.name }
                val adapter = ArrayAdapter(this@AddCustomerActivity, android.R.layout.simple_dropdown_item_1line, routeNames)
                binding.actRoute.setAdapter(adapter)
                binding.actRoute.setOnItemClickListener { _, _, position, _ ->
                    selectedRouteId = if (position == 0) null else routes.getOrNull(position - 1)?.id
                }
                if (selectedRouteId != null && binding.actRoute.text.isNullOrBlank()) {
                    binding.actRoute.setText(routes.firstOrNull { it.id == selectedRouteId }?.name.orEmpty(), false)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@AddCustomerActivity,
                    NetworkMessageUtils.friendlyMessage(e, "Failed to load routes"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun saveCustomer() {
        val name = binding.etCustomerName.text.toString().trim()
        val shopName = binding.etShopName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val retailerId = binding.etRetailerId.text.toString().trim()
        val city = binding.etCity.text.toString().trim()
        val state = binding.etState.text.toString().trim()
        val area = binding.etArea.text.toString().trim()
        val pincode = binding.etPinCode.text.toString().trim()
        val address = binding.etAddressLine1.text.toString().trim()
        
        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
            return
        }

        val request = AddCustomerRequest(
            id = customerToUpdate?.id,
            name = name,
            shop_name = shopName,
            phone = phone,
            city = city,
            state = state,
            area = area,
            pincode = pincode,
            address = address,
            retailer_id = retailerId,
            route_id = selectedRouteId
        )

        binding.btnAddCustomer.showLoading(true, "Saving...")
        showScreenLoading()

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { api.addOrUpdateCustomer(request) }
                binding.btnAddCustomer.showLoading(false)
                hideScreenLoading()
                if (response.success) {
                    Toast.makeText(this@AddCustomerActivity, response.message, Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@AddCustomerActivity, response.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.btnAddCustomer.showLoading(false)
                hideScreenLoading()
                Toast.makeText(
                    this@AddCustomerActivity,
                    NetworkMessageUtils.friendlyMessage(e, "Failed to save customer"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
