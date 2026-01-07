package com.svd.svdagencies.ui.admin.customer

import android.os.Bundle
import android.widget.Toast
import com.svd.svdagencies.R
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import com.svd.svdagencies.databinding.ActivityAddCustomerBinding

class AddCustomerActivity : AdminBaseActivity() {

    private lateinit var binding: ActivityAddCustomerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCustomerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup common admin toolbar with "Add Customer" title
        setupAdminLayout("Add Customer")

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnAddCustomer.setOnClickListener {
            // TODO: Implement API call to add customer
            Toast.makeText(this, "Add Customer API Integration Needed", Toast.LENGTH_SHORT).show()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }
}
