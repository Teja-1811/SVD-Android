package com.svd.svdagencies.ui.user

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.svd.svdagencies.R
import com.svd.svdagencies.ui.auth.LoginActivity
import com.svd.svdagencies.utils.SessionManager

class UserMainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user)

        // ================= TOOLBAR =================
        toolbar = findViewById(R.id.userToolbar)
        setSupportActionBar(toolbar)

        // LOGOUT
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_logout -> {
                    SessionManager(this).logout()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }

        // ================= BOTTOM NAV =================
        val bottomNav = findViewById<BottomNavigationView>(R.id.userBottomNav)

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
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_customer_toolbar, menu)
        return true
    }

    private fun loadFragment(fragment: Fragment, title: String) {
        toolbar.title = title
        supportFragmentManager.beginTransaction()
            .replace(R.id.userFragmentContainer, fragment)
            .commit()
    }
}
