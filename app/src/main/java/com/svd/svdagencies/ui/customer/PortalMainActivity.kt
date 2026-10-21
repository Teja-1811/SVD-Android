package com.svd.svdagencies.ui.customer

import android.content.Intent
import android.os.Bundle
import android.graphics.Rect
import android.view.WindowManager
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import com.svd.svdagencies.utils.SessionManager
import com.svd.svdagencies.utils.UserRole
import com.svd.svdagencies.base.BaseActivity

abstract class PortalMainActivity : BaseActivity() {

    protected abstract val expectedRole: String
    protected abstract fun createHomeFragment(): Fragment

    protected open val portalLayoutRes: Int = R.layout.customer
    protected open val portalBottomMenuRes: Int = R.menu.menu_customer_bottom
    protected open val portalDrawerMenuRes: Int = R.menu.menu_customer_drawer
    protected open val portalHomeTitle: String? = null
    protected open val portalDrawerTitle: String = "Customer Access"
    protected open val portalDrawerSubtitle: String = "Orders, support and invoices"

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvToolbarTitle: TextView
    private lateinit var btnMenu: ImageButton
    private lateinit var btnLogout: ImageButton
    private lateinit var bottomNav: BottomNavigationView
    private var activeTabId: Int? = null
    private var homeTitle = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SessionManager(this).getRole() != expectedRole) {
            startActivity(PortalNavigation.homeIntent(this))
            finish()
            return
        }
        setContentView(portalLayoutRes)

        // UI Binding
        drawerLayout = findViewById(R.id.customerDrawerLayout)
        navigationView = findViewById(R.id.customerNavigationView)
        toolbar = findViewById(R.id.customerToolbar)
        tvToolbarTitle = findViewById(R.id.tvCustomerToolbarTitle)
        btnMenu = findViewById(R.id.btnCustomerMenu)
        btnLogout = findViewById(R.id.btnCustomerLogout)
        bottomNav = findViewById(R.id.customerBottomNav)
        configureKeyboardInsets()

        if (portalBottomMenuRes != R.menu.menu_customer_bottom) {
            bottomNav.menu.clear()
            bottomNav.inflateMenu(portalBottomMenuRes)
        }
        if (portalDrawerMenuRes != R.menu.menu_customer_drawer) {
            navigationView.menu.clear()
            navigationView.inflateMenu(portalDrawerMenuRes)
        }

        homeTitle = portalHomeTitle ?: "Retailer Portal"
        tvToolbarTitle.text = homeTitle

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Toolbar Actions
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        val headerView = navigationView.getHeaderView(0)
        headerView.findViewById<TextView>(R.id.tvCustomerName).text = portalDrawerTitle
        headerView.findViewById<TextView>(R.id.tvCustomerDetail).text = portalDrawerSubtitle
        headerView.findViewById<View>(R.id.btnCloseDrawer).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        btnLogout.setOnClickListener {
            handleLogout()
        }

        PortalNavigation.configure(this, 0) { showHome() }

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> loadFragment(it.itemId, "customer_home", homeTitle) { createHomeFragment() }
                R.id.nav_companies -> loadFragment(it.itemId, "customer_companies", (if (expectedRole == UserRole.USER) "Products" else "Catalog")) { CustomerCompaniesFragment() }
                R.id.nav_orders -> loadFragment(it.itemId, "customer_orders", getString(R.string.title_orders)) { CustomerOrdersFragment.newInstance() }
                R.id.nav_bills -> loadFragment(it.itemId, "customer_bills", getString(R.string.title_bills)) { CustomerBillsFragment() }
                R.id.nav_subscriptions -> {
                    startActivity(Intent(this, CustomerSubscriptionsActivity::class.java))
                    return@setOnItemSelectedListener false
                }
                R.id.nav_payment -> loadFragment(it.itemId, "customer_payment", getString(R.string.title_payment)) { CustomerPaymentFragment() }
            }
            true
        }

        val restoredTab = savedInstanceState?.getInt("portal_tab", R.id.nav_home) ?: R.id.nav_home
        bottomNav.selectedItemId = intent.getIntExtra("user_tab", restoredTab)
        openSubscriptionPaymentIfRequested()
    }

    private fun configureKeyboardInsets() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val content = findViewById<View>(android.R.id.content)
        val divider = findViewById<View>(R.id.dividerBottom)
        var previousKeyboardHeight = 0
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            val keyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val gap = if (keyboardVisible) (8 * resources.displayMetrics.density).toInt() else 0
            view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, keyboard.bottom) + gap)
            bottomNav.visibility = if (keyboardVisible) View.GONE else View.VISIBLE
            divider.visibility = bottomNav.visibility
            if (keyboardVisible && keyboard.bottom != previousKeyboardHeight) {
                view.post {
                    currentFocus?.takeIf { it.isAttachedToWindow }?.let { focused ->
                        focused.requestRectangleOnScreen(Rect(0, 0, focused.width, focused.height), false)
                    }
                }
            }
            previousKeyboardHeight = keyboard.bottom
            // The portal already applied these insets; children must not add them again.
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(content)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::bottomNav.isInitialized) {
            bottomNav.selectedItemId = intent.getIntExtra("user_tab", R.id.nav_home)
            openSubscriptionPaymentIfRequested()
        }
    }

    fun openOrderPayment(orderId: Int, amount: Double) {
        bottomNav.selectedItemId = R.id.nav_payment
        val payment = CustomerPaymentFragment().apply {
            arguments = Bundle().apply { putInt("order_id", orderId); putDouble("order_amount", amount) }
        }
        supportFragmentManager.beginTransaction().replace(R.id.customerFragmentContainer, payment, "customer_payment").commit()
        activeTabId = R.id.nav_payment
        tvToolbarTitle.text = getString(R.string.title_payment)
    }

    private fun openSubscriptionPaymentIfRequested() {
        val subscriptionId = intent.getIntExtra("subscription_payment_id", -1)
        if (subscriptionId == -1) {
            val orderId = intent.getIntExtra("order_payment_id", -1)
            if (orderId != -1) {
                openOrderPayment(orderId, intent.getDoubleExtra("order_payment_amount", 0.0))
                intent.removeExtra("order_payment_id")
            }
            return
        }
        val payment = CustomerPaymentFragment().apply {
            arguments = Bundle().apply {
                putInt("subscription_id", subscriptionId)
                putDouble("subscription_amount", intent.getDoubleExtra("subscription_payment_amount", 0.0))
                putString("subscription_name", intent.getStringExtra("subscription_payment_name"))
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.customerFragmentContainer, payment, "customer_payment").commit()
        activeTabId = R.id.nav_payment
        tvToolbarTitle.text = getString(R.string.title_payment)
        intent.removeExtra("subscription_payment_id")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::bottomNav.isInitialized) outState.putInt("portal_tab", bottomNav.selectedItemId)
        super.onSaveInstanceState(outState)
    }

    private fun showHome() {
        bottomNav.selectedItemId = R.id.nav_home
        loadFragment(R.id.nav_home, "customer_home", homeTitle) { createHomeFragment() }
    }

    private fun handleLogout() {
        SessionManager(this).logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    fun openOrdersScreen(editLatestOrder: Boolean = false, productId: Int? = null) {
        bottomNav.menu.findItem(R.id.nav_orders).isChecked = true
        if (editLatestOrder || productId != null) {
            tvToolbarTitle.text = getString(R.string.title_orders)
            supportFragmentManager.beginTransaction()
                .replace(R.id.customerFragmentContainer, CustomerOrdersFragment.newInstance(editLatestOrder = editLatestOrder, productId = productId), "customer_orders_edit")
                .commit()
            activeTabId = R.id.nav_orders
        } else {
            loadFragment(R.id.nav_orders, "customer_orders", getString(R.string.title_orders)) { CustomerOrdersFragment.newInstance() }
        }
    }

    private fun loadFragment(itemId: Int, tag: String, title: String, createFragment: () -> Fragment) {
        if (activeTabId == itemId) {
            return
        }
        tvToolbarTitle.text = title
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        fragmentManager.fragments.forEach { transaction.hide(it) }

        val fragment = fragmentManager.findFragmentByTag(tag) ?: createFragment().also {
            transaction.add(R.id.customerFragmentContainer, it, tag)
        }
        transaction.show(fragment).commit()
        activeTabId = itemId
    }
}
