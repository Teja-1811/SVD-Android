package com.svd.svdagencies.ui.admin

import android.content.Intent
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.svd.svdagencies.R
import com.svd.svdagencies.ui.admin.bills.AdminBillsActivity
import com.svd.svdagencies.ui.admin.cashbook.AdminCashBookActivity
import com.svd.svdagencies.ui.admin.companies.AdminCompaniesActivity
import com.svd.svdagencies.ui.admin.customer.CustomersData
import com.svd.svdagencies.ui.admin.items.AdminItemsActivity
import com.svd.svdagencies.ui.admin.stock.AdminStockActivity
import com.svd.svdagencies.ui.auth.LoginActivity
import com.svd.svdagencies.base.BaseActivity
import com.svd.svdagencies.utils.SessionManager

abstract class AdminBaseActivity : BaseActivity() {

    protected lateinit var drawerLayout: DrawerLayout

    protected fun setupAdminLayout(title: String) {
        
        val dl = findViewById<DrawerLayout>(R.id.drawerLayout)
        if (dl != null) {
            drawerLayout = dl
        }

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.adminToolbar)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)

            ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
                val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
                view.updatePadding(top = statusBarInsets.top)
                insets
            }

            toolbar.findViewById<TextView>(R.id.tvToolbarTitle)?.text = title

            toolbar.findViewById<ImageButton>(R.id.btnMenu)?.setOnClickListener {
                if (::drawerLayout.isInitialized) {
                    drawerLayout.openDrawer(GravityCompat.START)
                }
            }

            toolbar.findViewById<ImageButton>(R.id.btnLogout)?.setOnClickListener {
                SessionManager(this).logout()
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
            }
        }

        val navView = findViewById<NavigationView>(R.id.navigationView)
        if (navView != null) {
            // Safe header access
            if (navView.headerCount > 0) {
                val headerView = navView.getHeaderView(0)
                headerView?.findViewById<ImageView>(R.id.btnCloseDrawer)?.setOnClickListener {
                    if (::drawerLayout.isInitialized) {
                        drawerLayout.closeDrawer(GravityCompat.START)
                    }
                }
            }
            
            ViewCompat.setOnApplyWindowInsetsListener(navView) { view, insets ->
                val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
                view.updatePadding(top = statusBarInsets.top)
                insets
            }

            navView.setNavigationItemSelectedListener { item ->
                if (::drawerLayout.isInitialized) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                }

                when (item.itemId) {
                    R.id.nav_home -> {
                        if (this !is AdminDashboardActivity) {
                            openAdminDestination(AdminDashboardActivity::class.java)
                        }
                        true
                    }
                    R.id.nav_companies -> {
                        if (this !is AdminCompaniesActivity) {
                            openAdminDestination(AdminCompaniesActivity::class.java)
                        }
                        true
                    }
                    R.id.nav_customers -> {
                        if (this !is CustomersData) {
                            openAdminDestination(CustomersData::class.java)
                        }
                        true
                    }
                    R.id.nav_bills -> {
                        if (this !is AdminBillsActivity) {
                            openAdminDestination(AdminBillsActivity::class.java)
                        }
                        true
                    }
                    R.id.nav_orders -> {
                        if (this !is AdminOrdersActivity) {
                            openAdminDestination(AdminOrdersActivity::class.java)
                        }
                        true
                    }
                    R.id.nav_delivery_dashboard -> {
                        if (this !is AdminUserDeliveryActivity) {
                            openAdminDestination(AdminUserDeliveryActivity::class.java)
                        }
                        true
                    }
                    R.id.nav_agent_dues -> {
                        if (this !is AdminDeliveryAgentDuesActivity) {
                            openAdminDestination(AdminDeliveryAgentDuesActivity::class.java)
                        }
                        true
                    }
                    R.id.nav_items -> {
                        if (this !is AdminItemsActivity) {
                            openAdminDestination(AdminItemsActivity::class.java)
                        }
                        true
                    }
                    R.id.nav_stock -> {
                        if (this !is AdminStockActivity) {
                            openAdminDestination(AdminStockActivity::class.java)
                        }
                        true
                    }
                    R.id.nav_subscriptions -> {
                        if (this !is AdminSubscriptionsActivity) {
                            openAdminDestination(AdminSubscriptionsActivity::class.java)
                        }
                        true
                    }
                    R.id.nav_dues -> {
                        if (this !is AdminDuesActivity) {
                            openAdminDestination(AdminDuesActivity::class.java)
                        }
                        true
                    }
                    R.id.nav_cashbook -> {
                        if (this !is AdminCashBookActivity) {
                             openAdminDestination(AdminCashBookActivity::class.java)
                        }
                        true
                    }
                    R.id.nav_monthly_summary -> {
                        if (this !is AdminMonthlySummary) {
                            openAdminDestination(AdminMonthlySummary::class.java)
                        }
                        true
                    }
                    R.id.nav_payments -> {
                        if (this !is AdminPaymentsActivity) {
                            openAdminDestination(AdminPaymentsActivity::class.java)
                        }
                        true
                    }
                    R.id.nav_enquiries -> {
                        if (this !is AdminEnquiriesActivity) {
                            openAdminDestination(AdminEnquiriesActivity::class.java)
                        }
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun openAdminDestination(activityClass: Class<*>) {
        val intent = Intent(this, activityClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
    }
}
