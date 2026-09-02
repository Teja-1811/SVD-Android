package com.svd.svdagencies.ui.customer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.svd.svdagencies.R
import com.svd.svdagencies.ui.auth.LoginActivity
import com.svd.svdagencies.ui.customer.CustomerStatementActivity
import com.svd.svdagencies.utils.SessionManager
import com.svd.svdagencies.base.BaseActivity

class TermsConditionsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.customer)

        val drawerLayout = findViewById<DrawerLayout>(R.id.customerDrawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.customerNavigationView)
        findViewById<ImageButton>(R.id.btnCustomerMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        navigationView.getHeaderView(0).findViewById<View>(R.id.btnCloseDrawer).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        navigationView.setCheckedItem(R.id.nav_terms)
        val session = SessionManager(this)
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    startActivity(Intent(this, CustomerMainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_terms -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_company -> {
                    openDrawerDestination(drawerLayout, CustomerCompanyDetailsActivity::class.java)
                    true
                }
                R.id.nav_support -> {
                    openDrawerDestination(drawerLayout, CustomerContactSupportActivity::class.java)
                    true
                }
                R.id.nav_queries -> {
                    openDrawerDestination(drawerLayout, CustomerRaisedQueriesActivity::class.java)
                    true
                }
                R.id.nav_statement -> {
                    openDrawerDestination(drawerLayout, CustomerStatementActivity::class.java)
                    true
                }
                R.id.nav_logout -> {
                    session.logout()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }

        findViewById<ImageButton>(R.id.btnCustomerLogout).setOnClickListener {
            session.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        findViewById<TextView>(R.id.tvCustomerToolbarTitle).text = "Terms & Conditions"
        findViewById<BottomNavigationView>(R.id.customerBottomNav).visibility = View.GONE

        val container = findViewById<FrameLayout>(R.id.customerFragmentContainer)
        layoutInflater.inflate(R.layout.activity_terms_conditions, container, true)

        setupAccordion(R.id.sectionOperator, R.id.operatorContent)
        setupAccordion(R.id.sectionOrders, R.id.ordersContent)
    }

    private fun setupAccordion(headerId: Int, contentId: Int) {
        val header = findViewById<TextView>(headerId)
        val content = findViewById<View>(contentId)

        header.setOnClickListener {
            if (content.visibility == View.VISIBLE) {
                content.visibility = View.GONE
            } else {
                content.visibility = View.VISIBLE
            }
        }
    }

    private fun openDrawerDestination(
        drawerLayout: DrawerLayout,
        activityClass: Class<out AppCompatActivity>
    ) {
        drawerLayout.closeDrawer(GravityCompat.START)
        drawerLayout.post {
            startActivity(Intent(this, activityClass))
        }
    }
}
