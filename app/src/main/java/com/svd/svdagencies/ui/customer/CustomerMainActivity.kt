package com.svd.svdagencies.ui.customer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.svd.svdagencies.R
import com.svd.svdagencies.ui.auth.LoginActivity
import com.svd.svdagencies.ui.customer.fragment.CustomerBillsFragment
import com.svd.svdagencies.ui.customer.fragment.CustomerCompaniesFragment
import com.svd.svdagencies.ui.customer.fragment.CustomerHomeFragment
import com.svd.svdagencies.ui.customer.fragment.CustomerOrdersFragment
import com.svd.svdagencies.ui.customer.fragment.CustomerPaymentFragment
import com.svd.svdagencies.ui.customer.CustomerCompanyDetailsActivity
import com.svd.svdagencies.ui.customer.CustomerContactSupportActivity
import com.svd.svdagencies.ui.customer.CustomerRaisedQueriesActivity
import com.svd.svdagencies.ui.user.TermsConditionsActivity
import com.svd.svdagencies.utils.SessionManager

class CustomerMainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvToolbarTitle: TextView
    private lateinit var btnMenu: ImageButton
    private lateinit var btnLogout: ImageButton
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.customer)

        // UI Binding
        drawerLayout = findViewById(R.id.customerDrawerLayout)
        navigationView = findViewById(R.id.customerNavigationView)
        toolbar = findViewById(R.id.customerToolbar)
        tvToolbarTitle = findViewById(R.id.tvCustomerToolbarTitle)
        btnMenu = findViewById(R.id.btnCustomerMenu)
        btnLogout = findViewById(R.id.btnCustomerLogout)
        bottomNav = findViewById(R.id.customerBottomNav)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Toolbar Actions
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        val headerView = navigationView.getHeaderView(0)
        headerView.findViewById<View>(R.id.btnCloseDrawer).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        btnLogout.setOnClickListener {
            handleLogout()
        }

        // Navigation Drawer Actions
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_terms -> {
                    openDrawerDestination(TermsConditionsActivity::class.java)
                    true
                }
                R.id.nav_company -> {
                    openDrawerDestination(CustomerCompanyDetailsActivity::class.java)
                    true
                }
                R.id.nav_support -> {
                    openDrawerDestination(CustomerContactSupportActivity::class.java)
                    true
                }
                R.id.nav_queries -> {
                    openDrawerDestination(CustomerRaisedQueriesActivity::class.java)
                    true
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            loadFragment(CustomerHomeFragment(), "Home")
        }

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> loadFragment(CustomerHomeFragment(), "Home")
                R.id.nav_companies -> loadFragment(CustomerCompaniesFragment(), "Companies")
                R.id.nav_orders -> loadFragment(CustomerOrdersFragment.newInstance(), "Orders")
                R.id.nav_bills -> loadFragment(CustomerBillsFragment(), "Bills")
                R.id.nav_payment -> loadFragment(CustomerPaymentFragment(), "Payment")
            }
            true
        }
    }

    private fun handleLogout() {
        SessionManager(this).logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    fun openOrdersScreen(editLatestOrder: Boolean = false) {
        bottomNav.menu.findItem(R.id.nav_orders).isChecked = true
        loadFragment(CustomerOrdersFragment.newInstance(editLatestOrder), "Orders")
    }

    private fun openDrawerDestination(activityClass: Class<out AppCompatActivity>) {
        drawerLayout.closeDrawer(GravityCompat.START)
        drawerLayout.post {
            startActivity(Intent(this, activityClass))
        }
    }

    private fun loadFragment(fragment: Fragment, title: String) {
        tvToolbarTitle.text = title
        supportFragmentManager.beginTransaction()
            .replace(R.id.customerFragmentContainer, fragment)
            .commit()
    }
}
