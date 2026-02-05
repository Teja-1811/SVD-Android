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

abstract class AdminBaseActivity : AppCompatActivity() {

    protected lateinit var drawerLayout: DrawerLayout

    protected fun setupAdminLayout(title: String) {
        
        // 1. Enable Edge-to-Edge: This allows the background to draw behind the status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)

        drawerLayout = findViewById(R.id.drawerLayout)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.adminToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // 2. Handle Insets: Add top padding to the toolbar so content is not hidden by the status bar
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.updatePadding(top = statusBarInsets.top)
            insets
        }

        val tvTitle = toolbar.findViewById<TextView>(R.id.tvToolbarTitle)
        val btnMenu = toolbar.findViewById<ImageButton>(R.id.btnMenu)
        val btnLogout = toolbar.findViewById<ImageButton>(R.id.btnLogout)

        tvTitle.text = title

        // Open Drawer
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Logout
        btnLogout.setOnClickListener {
            SessionManager(this).logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }

        val navView = findViewById<NavigationView>(R.id.navigationView)

        // ===== Drawer Header Close Button =====
        val headerView = navView.getHeaderView(0)
        val btnCloseDrawer = headerView.findViewById<ImageView>(R.id.btnCloseDrawer)

        btnCloseDrawer.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        
        // Handle insets for Navigation Drawer as well
        ViewCompat.setOnApplyWindowInsetsListener(navView) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.updatePadding(top = statusBarInsets.top)
            insets
        }

        // ===== Drawer Menu Clicks =====
        navView.setNavigationItemSelectedListener { item ->
            drawerLayout.closeDrawer(GravityCompat.START)

            when (item.itemId) {

                R.id.nav_home -> {
                    if (this !is AdminDashboardActivity) {
                        startActivity(Intent(this, AdminDashboardActivity::class.java))
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
                        startActivity(Intent(this, AdminOrdersActivity::class.java))
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
                R.id.nav_dues -> {
                    if (this !is AdminDuesActivity) {
                        startActivity(Intent(this, AdminDuesActivity::class.java))
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
                R.id.nav_payments -> {
                    if (this !is AdminPaymentsActivity) {
                        startActivity(Intent(this, AdminPaymentsActivity::class.java))
                    }
                    true
                }

                else -> false
            }
        }
    }
}
