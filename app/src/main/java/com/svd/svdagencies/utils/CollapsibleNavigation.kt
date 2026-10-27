package com.svd.svdagencies.utils

import android.app.Activity
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.chip.Chip
import com.svd.svdagencies.R

/** Group navigation destinations and reuse dashboard action handlers in the drawer. */
object CollapsibleNavigation {
    private val proxies = java.util.WeakHashMap<View, java.lang.ref.WeakReference<android.view.MenuItem>>()
    @JvmStatic fun applyTree(activity: Activity, root: View) {
        proxies[root]?.get()?.let { action ->
            if (action.isEnabled != root.isEnabled) action.isEnabled = root.isEnabled
            if (root is Chip && action.isChecked != root.isChecked) action.isChecked = root.isChecked
            if (root.visibility != View.GONE) root.visibility = View.GONE
        }
        if (root is NavigationView) { if (root.menu.size() > 0) install(activity, root); return }
        if (root is ViewGroup) for (index in 0 until root.childCount) applyTree(activity, root.getChildAt(index))
    }
    private data class Entry(val id: Int, val title: CharSequence, val icon: android.graphics.drawable.Drawable?, val checked: Boolean)
    private val groups = linkedMapOf(
        "Customers" to setOf("nav_customers", "nav_users", "nav_customer_item_discounts", "nav_payments"),
        "Bills & orders" to setOf("nav_bills", "nav_new_bill", "nav_orders", "nav_order_timings", "nav_delivery_home", "nav_delivery_bill_history", "nav_delivery_subscriptions", "nav_subscriptions", "nav_history_reports"),
        "Products & stock" to setOf("nav_items", "nav_stock", "nav_delivery_stock_entry", "nav_delivery_catalog", "nav_delivery_stock_history"),
        "Companies & routes" to setOf("nav_companies", "nav_routes"),
        "Finance & reports" to setOf("nav_dues", "nav_agent_dues", "nav_cashbook", "nav_expenses", "nav_statement", "nav_monthly_summary", "nav_category_sales", "nav_gst", "nav_purchase_reports", "nav_delivery_payments", "nav_delivery_today_report", "nav_delivery_overall_report"),
        "Offers & subscriptions" to setOf("nav_offers", "nav_offer_usage", "nav_subscription_report"),
        "Account" to setOf("nav_company", "nav_profile", "nav_support", "nav_queries", "nav_enquiries", "nav_change_theme", "nav_tools_appearance")
    )
    private val actions = mapOf(
        "chipDelivery" to "Customers", "chipRetailer" to "Customers", "chipUser" to "Customers", "fabAddCustomer" to "Customers",
        "btnAddItem" to "Products & stock", "btnDownloadBrochure" to "Products & stock", "btnManageStock" to "Products & stock", "btnLeakageReport" to "Products & stock", "btnRecordLeakage" to "Products & stock", "btnUpdateStock" to "Products & stock",
        "btnAddCompany" to "Companies & routes", "btnAddRoute" to "Companies & routes",
        "fabCreateBill" to "Bills & orders", "btnViewHistory" to "Bills & orders",
        "btnViewOrders" to "Bills & orders", "btnViewEnquiries" to "Account", "btnViewOverallReport" to "Finance & reports", "btnPdf" to "Finance & reports",
        "btnAddExpense" to "Finance & reports", "btnPayDeliverySalary" to "Finance & reports", "btnCommission" to "Finance & reports", "btnStatement" to "Finance & reports",
        "btnCreateOffer" to "Offers & subscriptions", "btnSubscriptionOffers" to "Offers & subscriptions", "btnCreateSubscriptionOffer" to "Offers & subscriptions",
        "userBrowseProducts" to "Bills & orders", "userSubscriptions" to "Bills & orders", "btnLatestOrderAction" to "Bills & orders"
    )

    @JvmStatic fun install(activity: Activity, navigation: NavigationView) {
        if (navigation.getTag(R.id.navigationView) == true) return
        navigation.setTag(R.id.navigationView, true)
        navigation.itemMaxLines = 2
        val root = activity.findViewById<View>(android.R.id.content)
        val actionViews = mutableListOf<Pair<View, String>>()
        fun collect(view: View) {
            if (view === navigation || view is androidx.recyclerview.widget.RecyclerView) return
            val name = if (view.id == View.NO_ID) "" else runCatching { view.resources.getResourceEntryName(view.id) }.getOrDefault("")
            val eligible = name != "btnAddItem" || activity is com.svd.svdagencies.ui.admin.items.AdminItemsActivity || root.findViewById<View>(R.id.rvItems) != null
            val label = (view as? TextView)?.text?.toString()?.takeIf { it.isNotBlank() } ?: view.contentDescription?.toString().orEmpty()
            val reportAction = view is android.widget.Button && (Regex("(?i)^(download|export)\\b").containsMatchIn(label) || name == "btnDownload" ||
                (activity is com.svd.svdagencies.ui.admin.AdminReportsActivity && label in setOf("Review purchase credit", "Generate past invoice GST", "Generate purchase invoice records")))
            val section = actions[name] ?: if (reportAction) when {
                activity.javaClass.simpleName.contains("Bill") || activity.javaClass.simpleName.contains("Order") -> "Bills & orders"
                else -> "Finance & reports"
            } else null
            if (eligible && section != null && view.visibility != View.GONE) actionViews += view to section
            if (view is ViewGroup) for (index in 0 until view.childCount) collect(view.getChildAt(index))
        }
        collect(root)
        val menu = navigation.menu
        val originals = (0 until menu.size()).map { menu.getItem(it) }.filter { it.isVisible && !it.hasSubMenu() }
            .map { Entry(it.itemId, it.title ?: "", it.icon, it.isChecked) }
        fun name(id: Int) = runCatching { activity.resources.getResourceEntryName(id) }.getOrDefault("")
        val byGroup = groups.mapValues { (_, ids) -> originals.filter { name(it.id) in ids } }
        val standalone = originals.filter { row -> groups.values.none { name(row.id) in it } }
        menu.clear()
        for (entry in standalone.filter { name(it.id) != "nav_logout" }) menu.add(0, entry.id, 0, entry.title).apply { icon = entry.icon; isCheckable = true; isChecked = entry.checked }
        val preferences = activity.getSharedPreferences("svd_navigation_groups", Activity.MODE_PRIVATE)
        fun closeDrawer() {
            var parent: View? = navigation
            while (parent != null && parent !is DrawerLayout) parent = parent.parent as? View
            (parent as? DrawerLayout)?.closeDrawer(GravityCompat.START)
        }
        val admin = originals.any { name(it.id) == "nav_customers" }
        for ((title, entries) in byGroup) {
            val local = actionViews.filter { it.second == title }
            if (entries.isEmpty() && local.isEmpty() && !(admin && title == "Customers")) continue
            val header = menu.add(0, View.generateViewId(), 0, title)
            header.isCheckable = false
            header.icon = entries.firstOrNull()?.icon
            val children = mutableListOf<android.view.MenuItem>()
            for (entry in entries) {
                if (local.any { name(it.first.id).startsWith("chip") } && name(entry.id) in setOf("nav_customers", "nav_users")) continue
                // The customer dashboard originally defaults to delivery agents.
                val label = if (admin && name(entry.id) == "nav_customers") "Delivery agents" else entry.title
                children += menu.add(0, entry.id, 0, "    $label").apply { icon = entry.icon; isCheckable = true; isChecked = entry.checked }
            }
            for ((view, _) in local) {
                val text = (view as? TextView)?.text?.takeIf { it.isNotBlank() } ?: view.contentDescription?.takeIf { it.isNotBlank() }
                    ?: name(view.id).removePrefix("btn").removePrefix("fab").replace(Regex("([a-z])([A-Z])"), "$1 $2")
                children += menu.add(0, View.generateViewId(), 0, "    $text").apply {
                    icon = (view as? android.widget.ImageView)?.drawable ?: (view as? com.google.android.material.button.MaterialButton)?.icon
                    isEnabled = view.isEnabled
                    isCheckable = view is Chip
                    isChecked = view is Chip && view.isChecked
                    setOnMenuItemClickListener { clicked ->
                        if (view.isEnabled) { closeDrawer(); view.performClick(); if (view is Chip) clicked.isChecked = view.isChecked }
                        true
                    }
                    proxies[view] = java.lang.ref.WeakReference(this)
                }
                view.visibility = View.GONE
                (view.parent as? ChipGroup)?.let { chipGroup ->
                    if ((0 until chipGroup.childCount).all { chipGroup.getChildAt(it).visibility == View.GONE }) chipGroup.visibility = View.GONE
                }
            }
            if (admin && title == "Customers" && local.isEmpty()) {
                children += menu.add(0, View.generateViewId(), 0, "    Retailers").apply {
                    setOnMenuItemClickListener {
                        closeDrawer()
                        activity.startActivity(Intent(activity, com.svd.svdagencies.ui.admin.customer.CustomersData::class.java).putExtra("DEFAULT_CUSTOMER_TYPE", "retailer"))
                        true
                    }
                }
                children += menu.add(0, View.generateViewId(), 0, "    Add customer").apply {
                    setOnMenuItemClickListener {
                        closeDrawer()
                        activity.startActivity(Intent(activity, com.svd.svdagencies.ui.admin.customer.AddCustomerActivity::class.java).putExtra("DEFAULT_CUSTOMER_TYPE", "retailer"))
                        true
                    }
                }
            }
            var expanded = preferences.getBoolean(title, false) || local.isNotEmpty() || entries.any { it.checked }
            fun render() {
                header.title = "$title  ${if (expanded) "▴" else "▾"}"
                androidx.core.view.MenuItemCompat.setContentDescription(header, "$title, ${if (expanded) "expanded" else "collapsed"}")
                children.forEach { it.isVisible = expanded }
            }
            header.setOnMenuItemClickListener { expanded = !expanded; preferences.edit().putBoolean(title, expanded).apply(); render(); true }
            render()
        }
        for (entry in standalone.filter { name(it.id) == "nav_logout" }) menu.add(0, entry.id, 0, entry.title).apply { icon = entry.icon }
    }
}
