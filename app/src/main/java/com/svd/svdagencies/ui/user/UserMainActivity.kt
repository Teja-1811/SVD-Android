package com.svd.svdagencies.ui.user

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.repository.UserRepository
import com.svd.svdagencies.ui.auth.LoginActivity
import com.svd.svdagencies.utils.RefreshManager
import com.svd.svdagencies.utils.SessionManager

class UserMainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var userProgress: ProgressBar
    private lateinit var tvStatusMessage: TextView
    private lateinit var session: SessionManager
    private var userId: Int = -1
    private var currentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user)

        toolbar = findViewById(R.id.userToolbar)
        setSupportActionBar(toolbar)

        swipeRefresh = findViewById(R.id.userSwipeRefresh)
        bottomNav = findViewById(R.id.userBottomNav)
        userProgress = findViewById(R.id.userProgress)
        tvStatusMessage = findViewById(R.id.tvUserStatusMessage)

        session = SessionManager(this)
        userId = session.getUserId()

        swipeRefresh.setOnRefreshListener {
            refreshDashboard(showLoader = false)
        }

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_logout -> {
                    handleLogout()
                    true
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            loadFragment(UserHomeFragment(), "Home")
        }

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> loadFragment(UserHomeFragment(), "Home")
                R.id.nav_subscription -> loadFragment(UserSubscriptionFragment(), "Subscriptions")
                R.id.nav_plans -> loadFragment(UserPlansFragment(), "Plans")
                R.id.nav_profile -> loadFragment(UserProfileFragment(), "Profile")
            }
            true
        }

        RefreshManager.startRefresh(swipeRefresh)
        refreshDashboard(showLoader = true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_customer_toolbar, menu)
        return true
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
            onSuccess = { data ->
                userProgress.visibility = View.GONE
                RefreshManager.stopRefresh(swipeRefresh)
                // TODO: move dashboard UI into fragments before re-adding summary binding
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
        currentFragment = fragment
        toolbar.title = title
        supportFragmentManager.beginTransaction()
            .replace(R.id.userFragmentContainer, fragment)
            .commit()
    }
}
