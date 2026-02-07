package com.svd.svdagencies.ui.admin

import android.content.Intent
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.svd.svdagencies.R
import com.svd.svdagencies.ui.auth.LoginActivity
import com.svd.svdagencies.utils.SessionManager
import com.svd.svdagencies.ui.admin.customer.CustomersData
import com.svd.svdagencies.ui.admin.bills.AdminBillsActivity
import com.svd.svdagencies.ui.admin.items.AdminItemsActivity
import com.svd.svdagencies.ui.admin.stock.AdminStockActivity
import com.svd.svdagencies.ui.admin.cashbook.AdminCashBookActivity
import com.svd.svdagencies.ui.admin.companies.AdminCompaniesActivity

abstract class AdminBaseActivity : AppCompatActivity() {

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
                            startActivity(Intent(this, AdminDashboardActivity::class.java))
                        }
                        true
                    }
                    R.id.nav_companies -> {
                        if (this !is AdminCompaniesActivity) {
                            startActivity(Intent(this, AdminCompaniesActivity::class.java))
                        }
                        true
                    }
                    R.id.nav_customers -> {
                        startActivity(Intent(this, CustomersData::class.java))
                        true
                    }
                    R.id.nav_bills -> {
                        if (this !is AdminBillsActivity) {
                            startActivity(Intent(this, AdminBillsActivity::class.java))
                        }
                        true
                    }
                    R.id.nav_orders -> {
                        if (this !is AdminOrdersActivity) {
                            if (this is AdminBaseActivity) {
                                // For activities not yet implemented or known
                                // startActivity(Intent(this, AdminOrdersActivity::class.java))
                            }
                        }
                        true
                    }
                    R.id.nav_items -> {
                        if (this !is AdminItemsActivity) {
                            startActivity(Intent(this, AdminItemsActivity::class.java))
                        }
                        true
                    }
                    R.id.nav_stock -> {
                        if (this !is AdminStockActivity) {
                            startActivity(Intent(this, AdminStockActivity::class.java))
                        }
                        true
                    }
                    R.id.nav_cashbook -> {
                        if (this !is AdminCashBookActivity) {
                             startActivity(Intent(this, AdminCashBookActivity::class.java))
                        }
                        true
                    }
                    R.id.nav_monthly_summary -> {
                        if (this !is AdminMonthlySummary) {
                            startActivity(Intent(this, AdminMonthlySummary::class.java))
                        }
                        true
                    }
                    else -> false
                }
            }
        }
    }
}
