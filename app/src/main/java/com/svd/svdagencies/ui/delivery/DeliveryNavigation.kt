package com.svd.svdagencies.ui.delivery

import android.app.Activity
import android.content.Intent
import android.widget.ImageButton
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.svd.svdagencies.R
import com.svd.svdagencies.ui.auth.LoginActivity
import com.svd.svdagencies.utils.SessionManager

object DeliveryNavigation {
    fun setup(
        activity: Activity,
        drawerLayout: DrawerLayout,
        navigationView: NavigationView,
        toolbar: MaterialToolbar? = null,
        menuButton: ImageButton? = null,
        selectedItemId: Int
    ) {
        toolbar?.setNavigationIcon(R.drawable.ic_menu)
        toolbar?.setNavigationOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        menuButton?.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        navigationView.setCheckedItem(selectedItemId)

        if (navigationView.headerCount > 0) {
            val headerView = navigationView.getHeaderView(0)
            headerView?.findViewById<android.widget.ImageView>(R.id.btnCloseDrawer)?.setOnClickListener {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        }

        navigationView.setNavigationItemSelectedListener { item ->
            drawerLayout.closeDrawer(GravityCompat.START)

            if (item.itemId == R.id.nav_logout) {
                handleLogout(activity)
                return@setNavigationItemSelectedListener true
            }

            val target = when (item.itemId) {
                R.id.nav_delivery_home -> DeliveryBillToCustomerActivity::class.java
                R.id.nav_delivery_stock_entry -> DeliveryStockEntryActivity::class.java
                R.id.nav_delivery_catalog -> DeliveryCatalogActivity::class.java
                R.id.nav_delivery_today_report -> DeliveryDashboardActivity::class.java
                R.id.nav_delivery_payments -> DeliveryCustomerPaymentsActivity::class.java
                R.id.nav_delivery_bill_history -> DeliveryBillHistoryActivity::class.java
                R.id.nav_delivery_stock_history -> DeliveryStockHistoryActivity::class.java
                R.id.nav_delivery_overall_report -> DeliveryOverallReportActivity::class.java
                else -> null
            }
            target?.let {
                if (activity::class.java != it) {
                    val intent = Intent(activity, it).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    activity.startActivity(intent)
                    activity.overridePendingTransition(0, 0)
                }
            }
            true
        }
    }

    private fun handleLogout(activity: Activity) {
        val sessionManager = SessionManager(activity)
        sessionManager.logout()
        val intent = Intent(activity, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        activity.startActivity(intent)
        activity.finish()
    }
}
