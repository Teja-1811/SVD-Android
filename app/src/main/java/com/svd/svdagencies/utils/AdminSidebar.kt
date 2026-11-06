package com.svd.svdagencies.utils

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.navigation.NavigationView
import com.svd.svdagencies.R
import com.svd.svdagencies.ui.admin.*
import com.svd.svdagencies.ui.admin.bills.*
import com.svd.svdagencies.ui.admin.cashbook.*
import com.svd.svdagencies.ui.admin.companies.*
import com.svd.svdagencies.ui.admin.customer.*
import com.svd.svdagencies.ui.admin.items.*
import com.svd.svdagencies.ui.admin.offers.*
import com.svd.svdagencies.ui.admin.routes.*
import com.svd.svdagencies.ui.admin.sales.CategorySalesActivity
import com.svd.svdagencies.ui.admin.stock.*

/** Fixed Milk Agency navigation; never derives admin links from screen buttons. */
object AdminSidebar {
    const val ACTION = "milk_sidebar_action"
    const val SELECTED = "milk_sidebar_selected"
    private data class Entry(val id: Int, val title: CharSequence, val icon: android.graphics.drawable.Drawable?)
    private data class Section(val entry: Entry, val children: List<Entry>)
    @JvmStatic fun install(activity: Activity, nav: NavigationView) {
        if (nav.getTag(R.id.admin_sidebar_installed) == true) return
        nav.setTag(R.id.admin_sidebar_installed, true)
        nav.itemMaxLines = 2
        // Section visibility changes should settle immediately without row animations.
        (nav.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView)?.itemAnimator = null
        nav.itemTextColor = ContextCompat.getColorStateList(activity, R.color.admin_sidebar_text)
        nav.itemIconTintList = ContextCompat.getColorStateList(activity, R.color.admin_sidebar_text)
        nav.setItemBackgroundResource(R.drawable.bg_admin_sidebar_item)
        nav.itemHorizontalPadding = dp(activity, 12)
        nav.itemVerticalPadding = dp(activity, 8)
        nav.layoutParams = nav.layoutParams.apply {
            width = minOf(dp(activity, 290), activity.resources.displayMetrics.widthPixels - dp(activity, 40))
        }
        val menu = nav.menu
        fun entry(item: MenuItem) = Entry(item.itemId, item.title ?: "", item.icon)
        val sections = (0 until menu.size()).map { index ->
            val row = menu.getItem(index)
            Section(entry(row), row.subMenu?.let { sub -> (0 until sub.size()).map { entry(sub.getItem(it)) } }.orEmpty())
        }
        val selected = selectedId(activity)
        val preferences = activity.getSharedPreferences("milk_agency_sidebar", Activity.MODE_PRIVATE)
        val sectionClosers = mutableMapOf<String, () -> Unit>()
        val initialSection = sections.firstOrNull { section -> section.children.any { it.id == selected } }
            ?.let { activity.resources.getResourceEntryName(it.entry.id) }
            ?: preferences.getString("expanded_section", null)
        menu.clear()
        for (section in sections) {
            if (section.children.isEmpty()) {
                menu.add(0, section.entry.id, 0, section.entry.title).apply {
                    icon = section.entry.icon; isCheckable = itemId != R.id.nav_logout; isChecked = itemId == selected
                }
                continue
            }
            val header = menu.add(0, section.entry.id, 0, section.entry.title).apply {
                icon = section.entry.icon; isCheckable = false
            }
            val indicator = TextView(activity).apply {
                textSize = 14f; setTextColor(ContextCompat.getColor(activity, R.color.admin_sidebar_ink))
                setPadding(dp(activity, 8), 0, dp(activity, 8), 0); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            header.actionView = indicator
            val children = section.children.map { row ->
                val label = android.text.SpannableString("    ${row.title}").apply {
                    setSpan(android.text.style.RelativeSizeSpan(0.92f), 0, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                menu.add(0, row.id, 0, label).apply {
                icon = row.icon; isCheckable = true; isChecked = row.id == selected
            } }
            val key = activity.resources.getResourceEntryName(header.itemId)
            var expanded = key == initialSection
            fun render() {
                indicator.text = if (expanded) "\u2212" else "+"
                androidx.core.view.MenuItemCompat.setContentDescription(header, "${section.entry.title}, ${if (expanded) "expanded" else "collapsed"}")
                // NavigationView refreshes its complete presenter for every menu-item change.
                // Suspend dispatch while updating a section so one tap produces one refresh.
                val menuBuilder = nav.menu as? androidx.appcompat.view.menu.MenuBuilder
                menuBuilder?.stopDispatchingItemsChanged()
                try {
                    children.forEach { it.isVisible = expanded }
                } finally {
                    menuBuilder?.startDispatchingItemsChanged()
                }
            }
            sectionClosers[key] = { expanded = false; render() }
            header.setOnMenuItemClickListener {
                val opening = !expanded
                sectionClosers.filterKeys { it != key }.values.forEach { it() }
                expanded = opening
                preferences.edit().putString("expanded_section", if (expanded) key else null).apply()
                // Refresh after NavigationView releases its click-time presenter suspension.
                nav.post { render() }
                true
            }
            render()
        }
    }
    private fun dp(activity: Activity, value: Int) = (value * activity.resources.displayMetrics.density).toInt()

    @JvmStatic fun intentFor(activity: Activity, id: Int): Intent? {
        fun target(type: Class<*>, action: Int = 0) = Intent(activity, type).apply {
            putExtra(SELECTED, id)
            if (action != 0) putExtra(ACTION, action)
        }
        return when (id) {
            R.id.nav_home -> target(AdminDashboardActivity::class.java)
            R.id.nav_customers -> target(CustomersData::class.java).putExtra("DEFAULT_CUSTOMER_TYPE", "delivery")
            R.id.nav_retailers -> target(CustomersData::class.java).putExtra("DEFAULT_CUSTOMER_TYPE", "retailer")
            R.id.nav_users -> target(AdminUsersActivity::class.java)
            R.id.nav_add_customer -> target(AddCustomerActivity::class.java)
            R.id.nav_deleted_customers -> target(AdminHistoryActivity::class.java).putExtra("history_kind", "deleted")
            R.id.nav_bills -> target(AdminBillsActivity::class.java)
            R.id.nav_new_bill -> target(CreateBillActivity::class.java)
            R.id.nav_orders -> target(AdminOrdersActivity::class.java)
            R.id.nav_orders_history -> target(AdminHistoryActivity::class.java).putExtra("history_kind", "orders")
            R.id.nav_order_timings -> target(AdminOrderTimingsActivity::class.java)
            R.id.nav_stock -> target(AdminStockActivity::class.java)
            R.id.nav_update_stock -> target(AdminStockUpdateActivity::class.java)
            R.id.nav_add_leakage -> target(AdminStockActivity::class.java, R.id.btnRecordLeakage)
            R.id.nav_items -> target(AdminItemsActivity::class.java)
            R.id.nav_add_item -> target(AddEditItemActivity::class.java)
            R.id.nav_customer_item_discounts -> target(CustomerItemDiscountActivity::class.java)
            R.id.nav_companies -> target(AdminCompaniesActivity::class.java)
            R.id.nav_add_company -> target(AddEditCompanyActivity::class.java)
            R.id.nav_routes -> target(AdminRoutesActivity::class.java)
            R.id.nav_add_route -> target(AdminRoutesActivity::class.java, R.id.btnAddRoute)
            R.id.nav_cashbook -> target(AdminCashBookActivity::class.java)
            R.id.nav_payment_summary -> target(AdminPaymentSummaryActivity::class.java)
            R.id.nav_dues -> target(AdminDuesActivity::class.java)
            R.id.nav_payments -> target(AdminPaymentsActivity::class.java)
            R.id.nav_expenses -> target(ViewExpensesActivity::class.java)
            R.id.nav_add_expense -> target(AddExpenseActivity::class.java)
            R.id.nav_add_commission -> target(AdminCashBookActivity::class.java, R.id.btnCommission)
            R.id.nav_statement -> target(AdminStatementActivity::class.java)
            R.id.nav_monthly_summary -> target(AdminMonthlySummary::class.java)
            R.id.nav_offer_usage -> target(AdminOfferUsageActivity::class.java)
            R.id.nav_subscription_report -> target(AdminSubscriptionsReportActivity::class.java)
            R.id.nav_add_offer -> target(AdminOffersActivity::class.java, R.id.btnCreateOffer)
            R.id.nav_add_subscription -> target(AdminSubscriptionOffersActivity::class.java, R.id.btnCreateSubscriptionOffer)
            R.id.nav_gst_configuration -> target(com.svd.svdagencies.ui.ToolsActivity::class.java).putExtra("settings_section", "gst")
            R.id.nav_appearance -> target(com.svd.svdagencies.ui.ToolsActivity::class.java).putExtra("settings_section", "appearance")
            R.id.nav_licences -> target(com.svd.svdagencies.ui.ToolsActivity::class.java).putExtra("settings_section", "licences")
            R.id.nav_enquiries -> target(AdminEnquiriesActivity::class.java)
            R.id.nav_category_sales -> target(CategorySalesActivity::class.java)
            R.id.nav_agent_dues -> target(AdminDeliveryAgentDuesActivity::class.java)
            R.id.nav_due_details -> target(AdminDeliveryAgentDuesActivity::class.java).putExtra("sidebar_due_details", true)
            R.id.nav_gst -> target(AdminGstActivity::class.java)
            else -> null
        }
    }
    @JvmStatic fun open(activity: AdminBaseActivity, id: Int): Boolean {
        if (id == R.id.nav_logout) { activity.logout(); return true }
        val destination = intentFor(activity, id) ?: return false
        // A new instance applies customer-type/action extras reliably on every screen.
        destination.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        activity.startActivity(destination)
        return true
    }

    /** The sidebar is the single navigation surface; retain only controls that act on the current screen. */
    @JvmStatic fun hideDashboardShortcuts(activity: Activity) {
        val shortcuts = when (activity) {
            is AdminDashboardActivity -> intArrayOf(R.id.btnUpdateStock, R.id.btnStatement)
            is AdminStockActivity -> intArrayOf(R.id.btnManageStock)
            is AdminItemsActivity -> intArrayOf(R.id.btnAddItem)
            is AdminCompaniesActivity -> intArrayOf(R.id.btnAddCompany)
            is AdminRoutesActivity -> intArrayOf(R.id.btnAddRoute)
            is AdminOffersActivity -> intArrayOf(R.id.btnCreateOffer)
            is AdminSubscriptionOffersActivity -> intArrayOf(R.id.btnCreateSubscriptionOffer)
            is AdminCashBookActivity -> intArrayOf(R.id.btnAddExpense, R.id.btnCommission)
            else -> return
        }
        shortcuts.forEach { activity.findViewById<View>(it)?.visibility = View.GONE }
    }
    @JvmStatic fun runPendingAction(activity: Activity, dataReady: Boolean = false) {
        val id = activity.intent.getIntExtra(ACTION, 0)
        if (id == 0) return
        if (!dataReady && (activity is AdminStockActivity || activity is AdminSubscriptionOffersActivity || activity is AdminCashBookActivity)) return
        activity.window.decorView.post {
            if (activity.isFinishing || activity.isDestroyed) return@post
            if (activity.intent.getIntExtra(ACTION, 0) != id) return@post
            val button = activity.findViewById<View>(id) ?: return@post
            if (button.isEnabled && button.hasOnClickListeners()) {
                activity.intent.removeExtra(ACTION)
                button.performClick()
            }
        }
    }
    private fun selectedId(activity: Activity): Int {
        val requested = activity.intent.getIntExtra(SELECTED, 0)
        if (requested != 0) return requested
        return when (activity) {
            is AdminDashboardActivity -> R.id.nav_home
            is AdminHistoryActivity -> if (activity.intent.getStringExtra("history_kind") == "deleted") R.id.nav_deleted_customers else R.id.nav_orders_history
            is AdminUsersActivity -> R.id.nav_users
            is CustomersData -> R.id.nav_customers
            is AddCustomerActivity -> R.id.nav_add_customer
            is CreateBillActivity -> R.id.nav_new_bill
            is AdminBillsActivity -> R.id.nav_bills
            is AdminOrdersActivity -> R.id.nav_orders
            is AdminOrderTimingsActivity -> R.id.nav_order_timings
            is AdminStockUpdateActivity -> R.id.nav_update_stock
            is AdminStockActivity -> R.id.nav_stock
            is AddEditItemActivity -> R.id.nav_add_item
            is CustomerItemDiscountActivity -> R.id.nav_customer_item_discounts
            is AdminItemsActivity -> R.id.nav_items
            is AddEditCompanyActivity -> R.id.nav_add_company
            is AdminCompaniesActivity -> R.id.nav_companies
            is AdminRoutesActivity -> R.id.nav_routes
            is AddExpenseActivity -> R.id.nav_add_expense
            is ViewExpensesActivity -> R.id.nav_expenses
            is AdminDuesActivity -> R.id.nav_dues
            is AdminPaymentsActivity -> R.id.nav_payments
            is AdminCashBookActivity -> R.id.nav_cashbook
            is AdminPaymentSummaryActivity -> R.id.nav_payment_summary
            is AdminStatementActivity -> R.id.nav_statement
            is AdminMonthlySummary -> R.id.nav_monthly_summary
            is AdminOfferUsageActivity -> R.id.nav_offer_usage
            is AdminSubscriptionsReportActivity -> R.id.nav_subscription_report
            is AdminOffersActivity -> R.id.nav_add_offer
            is AdminSubscriptionOffersActivity -> R.id.nav_add_subscription
            is AdminEnquiriesActivity -> R.id.nav_enquiries
            is CategorySalesActivity -> R.id.nav_category_sales
            is AdminDeliveryAgentDuesActivity -> if (activity.intent.getBooleanExtra("sidebar_due_details", false)) R.id.nav_due_details else R.id.nav_agent_dues
            is AdminGstActivity -> R.id.nav_gst
            else -> 0
        }
    }
}


