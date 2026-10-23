package com.svd.svdagencies.ui.customer

import android.content.Intent
import android.view.View
import android.widget.TextView
import android.widget.FrameLayout
import com.google.android.material.appbar.MaterialToolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.svd.svdagencies.R
import com.svd.svdagencies.ui.auth.LoginActivity
import com.svd.svdagencies.utils.SessionManager
import com.svd.svdagencies.utils.UserRole

/** Shared account pages retain the portal selected by the authenticated role. */
object PortalNavigation {
    fun homeIntent(activity: AppCompatActivity): Intent {
        val destination = when (SessionManager(activity).getRole()) {
            UserRole.USER -> UserPortalActivity::class.java
            UserRole.CUSTOMER -> CustomerMainActivity::class.java
            else -> LoginActivity::class.java
        }
        return Intent(activity, destination).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    /** Give standalone account pages the same toolbar and sidebar as the portal. */
    fun installPage(activity: AppCompatActivity, content: View, title: String, selectedItem: Int) {
        activity.setContentView(R.layout.customer)
        val toolbar = activity.findViewById<MaterialToolbar>(R.id.customerToolbar)
        activity.setSupportActionBar(toolbar)
        activity.supportActionBar?.setDisplayShowTitleEnabled(false)
        activity.findViewById<TextView>(R.id.tvCustomerToolbarTitle).text = title
        activity.findViewById<View>(R.id.customerBottomNav).visibility = View.GONE
        val drawer = activity.findViewById<DrawerLayout>(R.id.customerDrawerLayout)
        activity.findViewById<View>(R.id.btnCustomerMenu).setOnClickListener { drawer.openDrawer(GravityCompat.START) }
        activity.findViewById<View>(R.id.btnCustomerLogout).setOnClickListener {
            SessionManager(activity).logout()
            activity.startActivity(Intent(activity, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            activity.finish()
        }
        activity.findViewById<FrameLayout>(R.id.customerFragmentContainer).addView(content)
        configure(activity, selectedItem)
    }

    fun configure(activity: AppCompatActivity, selectedItem: Int, onHome: (() -> Unit)? = null) {
        val drawer = activity.findViewById<DrawerLayout>(R.id.customerDrawerLayout) ?: return
        val navigation = activity.findViewById<NavigationView>(R.id.customerNavigationView) ?: return
        val drawerWhite = android.graphics.Color.WHITE
        navigation.elevation = 0f
        navigation.backgroundTintList = android.content.res.ColorStateList.valueOf(drawerWhite)
        (navigation.background as? com.google.android.material.shape.MaterialShapeDrawable)?.apply {
            elevation = 0f
            fillColor = android.content.res.ColorStateList.valueOf(drawerWhite)
        }
        fun whitenMenu(view: android.view.View) {
            if (view is androidx.recyclerview.widget.RecyclerView) view.setBackgroundColor(drawerWhite)
            if (view is android.view.ViewGroup) {
                for (index in 0 until view.childCount) whitenMenu(view.getChildAt(index))
            }
        }
        whitenMenu(navigation)
        if (navigation.headerCount > 0) navigation.getHeaderView(0).setBackgroundColor(drawerWhite)

        val user = SessionManager(activity).getRole() == UserRole.USER
        navigation.menu.clear()
        navigation.inflateMenu(if (user) R.menu.menu_user_drawer else R.menu.menu_customer_drawer)
        navigation.setCheckedItem(selectedItem)
        navigation.getHeaderView(0).apply {
            findViewById<TextView>(R.id.tvCustomerName).text = if (user) "User Access" else "Retailer Access"
            findViewById<TextView>(R.id.tvCustomerDetail).text = if (user) "Orders, subscriptions and bills" else "Orders, support and invoices"
            findViewById<View>(R.id.btnCloseDrawer).setOnClickListener { drawer.closeDrawer(GravityCompat.START) }
        }
        navigation.setNavigationItemSelectedListener { item ->
            drawer.closeDrawer(GravityCompat.START)
            if (item.itemId == R.id.nav_home) {
                if (onHome != null) onHome() else {
                    activity.startActivity(homeIntent(activity))
                    activity.finish()
                }
                return@setNavigationItemSelectedListener true
            }
            if (item.itemId == selectedItem) return@setNavigationItemSelectedListener true
            if (item.itemId == R.id.nav_logout) {
                SessionManager(activity).logout()
                activity.startActivity(Intent(activity, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                activity.finish()
                return@setNavigationItemSelectedListener true
            }
            val target = when (item.itemId) {
                R.id.nav_tools_appearance -> com.svd.svdagencies.ui.ToolsActivity::class.java
                R.id.nav_terms -> TermsConditionsActivity::class.java
                R.id.nav_company -> CustomerCompanyDetailsActivity::class.java
                R.id.nav_profile -> CustomerProfileActivity::class.java
                R.id.nav_support -> CustomerContactSupportActivity::class.java
                R.id.nav_queries -> CustomerRaisedQueriesActivity::class.java
                R.id.nav_statement -> CustomerStatementActivity::class.java
                R.id.nav_history_reports -> CustomerHistoryActivity::class.java
                R.id.nav_subscriptions -> CustomerSubscriptionsActivity::class.java
                else -> null
            }
            if (target == null) false else {
                activity.startActivity(Intent(activity, target).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                true
            }
        }
    }
}
