package com.svd.svdagencies.ui.user

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.svd.svdagencies.R
import com.svd.svdagencies.ui.auth.LoginActivity
import com.svd.svdagencies.utils.SessionManager

class CompanyDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user)

        val drawerLayout = findViewById<DrawerLayout>(R.id.userDrawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.userNavigationView)
        findViewById<ImageButton>(R.id.btnUserMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        navigationView.getHeaderView(0).findViewById<android.view.View>(R.id.btnCloseDrawer).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        navigationView.setCheckedItem(R.id.nav_company)
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_terms -> {
                    openDrawerDestination(drawerLayout, TermsConditionsActivity::class.java)
                    true
                }
                R.id.nav_company -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_support -> {
                    openDrawerDestination(drawerLayout, ContactSupportActivity::class.java)
                    true
                }
                R.id.nav_queries -> {
                    openDrawerDestination(drawerLayout, RaisedQueriesActivity::class.java)
                    true
                }
                else -> false
            }
        }

        val session = SessionManager(this)
        findViewById<ImageButton>(R.id.btnUserLogout).setOnClickListener {
            session.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        findViewById<TextView>(R.id.tvUserToolbarTitle).text = "Company Details"

        findViewById<SwipeRefreshLayout>(R.id.userSwipeRefresh).isEnabled = false
        findViewById<BottomNavigationView>(R.id.userBottomNav).visibility = android.view.View.GONE
        findViewById<ProgressBar>(R.id.userProgress).visibility = android.view.View.GONE
        findViewById<TextView>(R.id.tvUserStatusMessage).visibility = android.view.View.GONE

        val container = findViewById<FrameLayout>(R.id.userFragmentContainer)
        layoutInflater.inflate(R.layout.activity_company_details, container, true)

        findViewById<MaterialButton>(R.id.btnOpenMap).setOnClickListener {
            val gmmIntentUri = Uri.parse("geo:0,0?q=Sri+Vijaya+Durga+Milk+Agencies+Gundugolanu")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
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
