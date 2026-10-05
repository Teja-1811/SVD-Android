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
    private val customerTypes = listOf("retailer", "user", "delivery")
    private var selectedType = "retailer"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdminCustomerAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        api = ApiClient.adminCustomerDashboard
        binding.spinnerEntryType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, customerTypes.map { it.replaceFirstChar(Char::titlecase) })

        // Check if we are in Update mode
        customerToUpdate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("CUSTOMER_TO_UPDATE", CustomerItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("CUSTOMER_TO_UPDATE")
        }
        selectedType = intent.getStringExtra("DEFAULT_CUSTOMER_TYPE")
            ?.takeIf { it in customerTypes }
            ?: selectedType

        if (customerToUpdate != null) {
            setupAdminLayout("Update Customer")
            binding.btnAddCustomer.text = "Update Customer"
            populateFields(customerToUpdate!!)
        } else {
            setupAdminLayout("Add Customer")
            binding.btnAddCustomer.text = "Add Customer"
            binding.etPinCode.setText("534427")
            binding.etCity.setText("GGL")
            binding.etState.setText("AP")
            binding.spinnerEntryType.setSelection(customerTypes.indexOf(selectedType))
        }

        loadRoutes()
        setupListeners()
        updateTypeLabels()
    }

    private fun populateFields(customer: CustomerItem) {
        selectedType = customer.user_type?.takeIf { it in customerTypes } ?: "retailer"
        binding.spinnerEntryType.setSelection(customerTypes.indexOf(selectedType))
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
                selectedType = detail.user_type?.takeIf { it in customerTypes } ?: selectedType
                binding.spinnerEntryType.setSelection(customerTypes.indexOf(selectedType))
                updateTypeLabels()
            } catch (e: Exception) {
                // Ignore failure
            }
        }
    }

    private fun setupListeners() {
        binding.spinnerEntryType.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedType = customerTypes[position]
                updateTypeLabels()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
        binding.btnAddCustomer.setOnClickListener {
            saveCustomer()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun updateTypeLabels() {
        val label = selectedType.replaceFirstChar(Char::titlecase)
        binding.tvFormSubtitle.text = "Fill the form to save ${selectedType} details"
        binding.tvCustomerNameLabel.text = "$label Name *"
        binding.tvShopNameLabel.text = if (selectedType == "user") "Reference / Shop Name" else "Shop Name"
        binding.tvRetailerIdLabel.text = "$label ID"
    }

    private fun loadRoutes() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { ApiClient.deliveryApi.getRoutes().awaitResponse() }
                routes = response.body().orEmpty()
                val routeNames = listOf("No Route") + routes.map { it.name }
                val adapter = ArrayAdapter(this@AddCustomerActivity, android.R.layout.simple_dropdown_item_1line, routeNames)
                binding.actRoute.setAdapter(adapter)
                binding.actRoute.setOnClickListener {
                    binding.actRoute.showDropDown()
                }
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
        val pinCode = binding.etPinCode.text.toString().trim()
        val flatNumber = binding.etAddressLine1.text.toString().trim()
        
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
            pinCode = pinCode,
            flatNumber = flatNumber,
            retailer_id = retailerId,
            route_id = selectedRouteId,
            userType = selectedType
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
