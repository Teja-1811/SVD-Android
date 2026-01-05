package com.svd.svdagencies.ui.customer

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.svd.svdagencies.R
import com.svd.svdagencies.ui.auth.LoginActivity
import com.svd.svdagencies.ui.customer.fragment.CustomerBillsFragment
import com.svd.svdagencies.ui.customer.fragment.CustomerHomeFragment
import com.svd.svdagencies.ui.customer.fragment.CustomerOrdersFragment
import com.svd.svdagencies.ui.customer.fragment.CustomerPaymentFragment
import com.svd.svdagencies.utils.SessionManager

class CustomerMainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_main)

        // ================= TOOLBAR =================
        toolbar = findViewById<MaterialToolbar>(R.id.customerToolbar)
        setSupportActionBar(toolbar)

        // LOGOUT
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_logout -> {
                    SessionManager(this).logout()

                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // ================= BOTTOM NAV =================
        val bottomNav = findViewById<BottomNavigationView>(R.id.customerBottomNav)

        if (savedInstanceState == null) {
            loadFragment(CustomerHomeFragment(), "Home")
        }

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> loadFragment(CustomerHomeFragment(), "Home")
                R.id.nav_orders -> loadFragment(CustomerOrdersFragment(), "Orders")
                R.id.nav_bills -> loadFragment(CustomerBillsFragment(), "Bills")
                R.id.nav_payment -> loadFragment(CustomerPaymentFragment(), "Payment")
            }
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_customer_toolbar, menu)
        return true
    }

    private fun loadFragment(fragment: Fragment, title: String) {
        toolbar.title = title
        supportFragmentManager.beginTransaction()
            .replace(R.id.customerFragmentContainer, fragment)
            .commit()
    }
}
