package com.svd.svdagencies.ui.admin.stock

import android.os.Bundle
import com.svd.svdagencies.R
import com.svd.svdagencies.ui.admin.AdminBaseActivity

class AdminStockUpdateActivity : AdminBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_stock_update_activity)
        setupAdminLayout("Update Stock")
    }
}