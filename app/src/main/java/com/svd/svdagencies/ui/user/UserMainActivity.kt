package com.svd.svdagencies.ui.user

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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

        // UI Binding
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

        // Toolbar Actions
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        btnLogout.setOnClickListener {
            handleLogout()
        }

        // Navigation Drawer Actions
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_terms -> {
                    startActivity(Intent(this, TermsConditionsActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_company -> {
                    startActivity(Intent(this, CompanyDetailsActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_support -> {
                    startActivity(Intent(this, ContactSupportActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> false
            }
        }

        swipeRefresh.setOnRefreshListener {
            refreshDashboard(showLoader = false)
        }

        if (savedInstanceState == null) {
            loadFragment(UserHomeFragment(), "SVD Agency")
        }

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> loadFragment(UserHomeFragment(), "SVD Agency")
                R.id.nav_subscription -> loadFragment(UserSubscriptionFragment(), "Subscriptions")
                R.id.nav_plans -> loadFragment(UserPlansFragment(), "Explore Plans")
                R.id.nav_profile -> loadFragment(UserProfileFragment(), "My Profile")
            }
            true
        }

        RefreshManager.startRefresh(swipeRefresh)
        refreshDashboard(showLoader = true)
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

    private fun loadFragment(fragment: Fragment, title: String) {
        tvToolbarTitle.text = title
        supportFragmentManager.beginTransaction()
            .replace(R.id.userFragmentContainer, fragment)
            .commit()
    }
}
