package com.svd.svdagencies.ui.admin

import android.os.Bundle
import com.svd.svdagencies.R
import com.svd.svdagencies.databinding.AdminCustomerPaymentBinding

class AdminPaymentsActivity : AdminBaseActivity() {

    private lateinit var binding: AdminCustomerPaymentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdminCustomerPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAdminLayout("Payments")
        
        // Additional initialization logic for payments can go here
    }
}
