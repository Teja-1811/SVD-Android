package com.svd.svdagencies.ui.admin.customer

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import com.svd.svdagencies.data.api.admin.CustomerDashboardApi
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.customerData.AddCustomerRequest
import com.svd.svdagencies.data.model.admin.customerData.CustomerItem
import com.svd.svdagencies.databinding.AdminCustomerAddBinding
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import com.svd.svdagencies.utils.NetworkMessageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddCustomerActivity : AdminBaseActivity() {

    private lateinit var binding: AdminCustomerAddBinding
    private var customerToUpdate: CustomerItem? = null
    private lateinit var api: CustomerDashboardApi

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

        setupListeners()
    }

    private fun populateFields(customer: CustomerItem) {
        binding.etCustomerName.setText(customer.name)
        binding.etShopName.setText(customer.shop_name)
        binding.etPhone.setText(customer.phone)
        
        if (customer.id != null) {
             fetchFullDetailsAndPopulate(customer.id)
        }
    }

    private fun fetchFullDetailsAndPopulate(id: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val detail = api.getCustomerDetail(id)
                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        binding.etCity.setText(detail.city)
                        binding.etState.setText(detail.state)
                        binding.etRetailerId.setText(detail.retailer_id)
                        binding.etArea.setText(detail.area)
                        binding.etPinCode.setText(detail.pincode)
                        binding.etAddressLine1.setText(detail.address)
                    }
                }
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
            customer_id = customerToUpdate?.id,
            name = name,
            shop_name = shopName,
            phone = phone,
            city = city,
            state = state,
            area = area,
            pincode = pincode,
            address = address,
            retailer_id = retailerId
        )

        binding.btnAddCustomer.isEnabled = false
        showScreenLoading()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.addOrUpdateCustomer(request)
                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        binding.btnAddCustomer.isEnabled = true
                        hideScreenLoading()
                        if (response.success) {
                            Toast.makeText(this@AddCustomerActivity, response.message, Toast.LENGTH_SHORT).show()
                            setResult(Activity.RESULT_OK)
                            finish()
                        } else {
                            Toast.makeText(this@AddCustomerActivity, response.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        binding.btnAddCustomer.isEnabled = true
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
    }
}
