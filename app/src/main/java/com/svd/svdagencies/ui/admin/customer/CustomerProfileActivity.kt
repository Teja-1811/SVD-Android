package com.svd.svdagencies.ui.admin.customer

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.admin.CustomerDashboardApi
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.CustomerDetail
import com.svd.svdagencies.data.model.admin.CustomerItem
import com.svd.svdagencies.databinding.ActivityCustomerProfileBinding
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomerProfileActivity : AdminBaseActivity() {

    private lateinit var binding: ActivityCustomerProfileBinding
    private var customer: CustomerItem? = null
    private lateinit var api: CustomerDashboardApi
    private var isFrozen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        api = ApiClient.adminCustomerDashboard

        // Setup common admin toolbar
        setupAdminLayout("Customer Profile")

        // Retrieve customer data from intent
        customer = intent.getParcelableExtra("CUSTOMER_DATA")

        if (customer != null) {
            setupInitialViews(customer!!)
            Log.d("CustomerProfile", "Fetching details for ID: ${customer?.id}")
            loadFullDetails(customer!!.id ?: 0)
        } else {
            Toast.makeText(this, "Error loading customer data", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupInitialViews(customer: CustomerItem) {
        // Populate basic views with customer data from list
        binding.cardProfile.txtCustomerName.text = "${customer.serial_no}. ${customer.name}"
        binding.cardProfile.txtShopName.text = customer.shop_name
        binding.cardProfile.txtPhone.text = customer.phone
        binding.cardProfile.txtBalance.text = "₹ %.2f".format(customer.due)

        // Initial frozen state from list item
        isFrozen = customer.frozen == true
        updateFreezeButtonState()

        // Setup button listeners
        binding.btnUpdate.setOnClickListener {
            Toast.makeText(this, "Update feature coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnFreeze.setOnClickListener {
             customer?.id?.let { id -> toggleFreeze(id) }
        }
    }

    private fun loadFullDetails(id: Int) {
        if (id == 0) return

        binding.progressBar.visibility = View.VISIBLE
        binding.cardDetails.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val detail = api.getCustomerDetail(id)
                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        binding.progressBar.visibility = View.GONE
                        displayFullDetails(detail)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        binding.progressBar.visibility = View.GONE
                        val errorMsg = e.message ?: "Unknown error"
                        Log.e("CustomerProfile", "Failed to load details", e)
                        Toast.makeText(this@CustomerProfileActivity, "Load failed: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun displayFullDetails(detail: CustomerDetail) {
        binding.cardDetails.visibility = View.VISIBLE

        binding.txtCustomerId.text = detail.id.toString()
        binding.txtRetailerId.text = detail.retailer_id ?: "-"
        binding.txtCity.text = detail.city ?: "-"
        binding.txtState.text = detail.state ?: "-"
        
        // Sync frozen state with details
        isFrozen = detail.frozen
        updateFreezeButtonState()
    }

    private fun updateFreezeButtonState() {
        binding.txtStatus.text = if (isFrozen) "Frozen" else "Active"
        binding.btnFreeze.text = if (isFrozen) "Unfreeze" else "Freeze"
        
        // Update color optionally
        val color = if (isFrozen) R.color.icon_green else R.color.icon_red
        // binding.btnFreeze.setBackgroundColor(...) - usually requires handling tint
    }

    private fun toggleFreeze(id: Int) {
        // Disable button to prevent double clicks
        binding.btnFreeze.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.toggleFreeze(id)
                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        binding.btnFreeze.isEnabled = true
                        if (response.success) {
                            isFrozen = response.frozen
                            updateFreezeButtonState()
                            Toast.makeText(this@CustomerProfileActivity, "Status updated", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@CustomerProfileActivity, "Failed to update status", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        binding.btnFreeze.isEnabled = true
                        Toast.makeText(this@CustomerProfileActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
