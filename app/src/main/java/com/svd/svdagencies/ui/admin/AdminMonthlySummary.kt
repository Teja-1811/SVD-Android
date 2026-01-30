package com.svd.svdagencies.ui.admin

import android.os.Bundle
import com.svd.svdagencies.R

class AdminMonthlySummary : AdminBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set a layout for this activity. If admin_customer_monthly_summary is suitable:
        setContentView(R.layout.admin_customer_monthly_summary)
        setupAdminLayout("Monthly Summary")
    }
}