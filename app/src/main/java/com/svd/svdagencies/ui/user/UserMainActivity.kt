package com.svd.svdagencies.ui.user

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.content.Intent
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.repository.UserRepository
import com.svd.svdagencies.ui.auth.LoginActivity
import com.svd.svdagencies.utils.RefreshManager
import com.svd.svdagencies.utils.SessionManager

class UserMainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvToolbarTitle: TextView
    private lateinit var btnMenu: ImageButton
    private lateinit var btnLogout: ImageButton
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var userProgress: ProgressBar
    private lateinit var tvStatusMessage: TextView
    private lateinit var session: SessionManager
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user)

        session = SessionManager(this)
        userId = session.getUserId()

        drawerLayout = findViewById(R.id.userDrawerLayout)
        navigationView = findViewById(R.id.userNavigationView)
        toolbar = findViewById(R.id.userToolbar)
        tvToolbarTitle = findViewById(R.id.tvUserToolbarTitle)
        btnMenu = findViewById(R.id.btnUserMenu)
        btnLogout = findViewById(R.id.btnUserLogout)
        swipeRefresh = findViewById(R.id.userSwipeRefresh)
        bottomNav = findViewById(R.id.userBottomNav)
        userProgress = findViewById(R.id.userProgress)
        tvStatusMessage = findViewById(R.id.tvUserStatusMessage)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        navigationView.getHeaderView(0).findViewById<View>(R.id.btnCloseDrawer).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        btnLogout.setOnClickListener {
            handleLogout()
        }

        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_terms -> {
                    openDrawerDestination(TermsConditionsActivity::class.java)
                    true
                }
                R.id.nav_company -> {
                    openDrawerDestination(CompanyDetailsActivity::class.java)
                    true
                }
                R.id.nav_support -> {
                    openDrawerDestination(ContactSupportActivity::class.java)
                    true
                }
                R.id.nav_queries -> {
                    openDrawerDestination(RaisedQueriesActivity::class.java)
                    true
                }
                else -> false
            }
        }

        swipeRefresh.setOnRefreshListener {
            refreshDashboard(showLoader = false)
        }

        if (savedInstanceState == null) {
            navigateToTab(R.id.nav_home)
        }

        bottomNav.setOnItemSelectedListener {
            navigateToTab(it.itemId)
            true
        }

        RefreshManager.startRefresh(swipeRefresh)
        refreshDashboard(showLoader = true)
    }

    fun navigateToTab(itemId: Int) {
        if (bottomNav.selectedItemId != itemId) {
            bottomNav.selectedItemId = itemId
            return
        }
        showFragmentForMenuItem(itemId)
    }

    private fun showFragmentForMenuItem(itemId: Int) {
        when (itemId) {
            R.id.nav_home -> loadFragment(UserHomeFragment(), "SVD Agency")
            R.id.nav_subscription -> loadFragment(UserSubscriptionFragment(), "Subscriptions")
            R.id.nav_bills -> loadFragment(UserBillsFragment(), "My Bills")
            R.id.nav_cart -> loadFragment(UserCartFragment(), "Cart")
            R.id.nav_profile -> loadFragment(UserProfileFragment(), "My Profile")
        }
    }

    private fun refreshDashboard(showLoader: Boolean) {
        if (userId == -1) {
            tvStatusMessage.text = "Unable to read user session."
            tvStatusMessage.visibility = View.VISIBLE
            RefreshManager.stopRefresh(swipeRefresh)
            return
        }

        if (showLoader) {
            userProgress.visibility = View.VISIBLE
        }

        tvStatusMessage.visibility = View.GONE

        UserRepository.fetchDashboard(
            userId = userId,
            onSuccess = {
                userProgress.visibility = View.GONE
                RefreshManager.stopRefresh(swipeRefresh)
            },
            onError = { message ->
                userProgress.visibility = View.GONE
                RefreshManager.stopRefresh(swipeRefresh)
                tvStatusMessage.text = message
                tvStatusMessage.visibility = View.VISIBLE
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun handleLogout() {
        session.logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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
            .replace(R.id.userFragmentContainer, fragment)
            .commit()
    }
}
