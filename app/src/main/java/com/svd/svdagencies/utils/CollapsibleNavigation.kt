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
        if (root.id == android.R.id.content) hideDuplicateDestinations(root)

        proxies[root]?.get()?.let { action ->
            if (action.isEnabled != root.isEnabled) action.isEnabled = root.isEnabled
            if (root is Chip && action.isChecked != root.isChecked) action.isChecked = root.isChecked
            if (root.visibility != View.GONE) root.visibility = View.GONE
        }
        if (root is NavigationView) {
            if (root.menu.size() > 0) {
                install(activity, root)
                SidebarLogoutFooter.install(root)
                SidebarTreeStyle.install(root)
            }
            return
        }
        if (root is ViewGroup) for (index in 0 until root.childCount) applyTree(activity, root.getChildAt(index))
    }
    private fun hideDuplicateDestinations(root: View) {
        val labels = mutableSetOf<String>()
        val destinations = mutableSetOf<String>()
        val selectedLabels = mutableSetOf<String>()
        val selectedDestinations = mutableSetOf<String>()
        fun normalize(text: CharSequence?) = text?.toString()?.lowercase(java.util.Locale.ROOT)
            ?.replace(Regex("[^a-z0-9]"), "").orEmpty()
        fun readMenu(menu: android.view.Menu) {
            for (index in 0 until menu.size()) {
                val item = menu.getItem(index)
                // Collapsed children remain valid destinations.
                if (item.isVisible || item.isCheckable) {
                    labels += normalize(item.title)
                    if (item.isChecked) selectedLabels += normalize(item.title)
                    if (item.itemId != View.NO_ID && item.itemId != 0) {
                        runCatching { root.resources.getResourceEntryName(item.itemId) }.getOrNull()?.let { destinations += it; if (item.isChecked) selectedDestinations += it }
                    }
                }
                item.subMenu?.let { readMenu(it) }
            }
        }
        fun findMenus(view: View) {
            if (view is NavigationView) { readMenu(view.menu); return }
            if (view is ViewGroup && view !is androidx.recyclerview.widget.RecyclerView)
                for (index in 0 until view.childCount) findMenus(view.getChildAt(index))
        }
        findMenus(root)
        if (labels.isEmpty()) return
        val aliases = mapOf(
            "btnCustomerLogout" to "nav_logout", "btnLogout" to "nav_logout",
            "userSubscriptions" to "nav_subscriptions", "btnViewOrders" to "nav_orders",
            "btnViewEnquiries" to "nav_enquiries", "btnViewOverallReport" to "nav_delivery_overall_report",
            "btnStatement" to "nav_statement", "btnUpdateStock" to "nav_update_stock",
            "btnRecordLeakage" to "nav_add_leakage", "btnManageStock" to "nav_update_stock",
            "btnAddItem" to "nav_add_item", "btnAddCompany" to "nav_add_company",
            "btnAddRoute" to "nav_add_route", "fabAddCustomer" to "nav_add_customer",
            "fabCreateBill" to "nav_new_bill", "btnAddExpense" to "nav_add_expense",
            "btnCommission" to "nav_add_commission", "btnCreateOffer" to "nav_add_offer",
            "btnCreateSubscriptionOffer" to "nav_add_subscription"
        )
        fun hide(view: View) {
            if (view is NavigationView || view is androidx.recyclerview.widget.RecyclerView) return
            val name = runCatching { view.resources.getResourceEntryName(view.id) }.getOrDefault("")
            // These controls operate on the current record or form, even when labels coincide.
            val protected = (name == "btnAddItem" && root.findViewById<View>(R.id.rvBillItems) != null) || name.contains("Save", true) || name.contains("Submit", true) ||
                name.contains("Generate", true) || name.contains("Confirm", true) ||
                name == "btnAddCustomer" || name == "btnLatestOrderAction" || name.contains("Delete", true) || name.contains("Edit", true)
            val isButton = view is android.widget.Button || view is android.widget.ImageButton
            val destination = aliases[name]
            val exactMatch = isButton && (normalize((view as? TextView)?.text).let { it.isNotEmpty() && it in labels && it !in selectedLabels } ||
                normalize(view.contentDescription).let { it.isNotEmpty() && it in labels && it !in selectedLabels })
            if (!protected && ((destination != null && destination in destinations && destination !in selectedDestinations) || exactMatch)) view.visibility = View.GONE
            if (view is ViewGroup) for (index in 0 until view.childCount) hide(view.getChildAt(index))
        }
        hide(root)
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
        if (activity is com.svd.svdagencies.ui.admin.AdminBaseActivity && (navigation.menu.findItem(R.id.nav_section_customer) != null || navigation.getTag(R.id.admin_sidebar_installed) == true)) {
            AdminSidebar.install(activity, navigation); return
        }
        if (navigation.getTag(R.id.navigationView) == true) return
        navigation.setTag(R.id.navigationView, true)
        if (navigation.menu.findItem(R.id.nav_delivery_home) != null) {
            val menu = navigation.menu
            val stock = menu.findItem(R.id.nav_delivery_stock_entry)
            val sub = stock?.subMenu
            if (sub != null) {
                val entries = (0 until sub.size()).map { sub.getItem(it) }.map { Entry(it.itemId, it.title ?: "", it.icon, it.isChecked) }
                val destinations = (0 until menu.size()).map { menu.getItem(it) }
                    .filter { it.itemId != R.id.nav_delivery_stock_entry }
                    .map { Entry(it.itemId, it.title ?: "", it.icon, it.isChecked) to it.isVisible }
                menu.clear()
                destinations.forEachIndexed { index, (row, visible) ->
                    menu.add(0, row.id, if (row.id == R.id.nav_delivery_home) 0 else 10 + index, row.title).apply {
                        icon = row.icon; isCheckable = row.id != R.id.nav_logout; isChecked = row.checked; isVisible = visible
                    }
                }
                val header = menu.add(0, R.id.nav_delivery_stock_entry, 1, "Daily Stock Entry").apply {
                    icon = androidx.core.content.ContextCompat.getDrawable(activity, R.drawable.ic_stock)
                    isCheckable = false
                }
                val children = entries.mapIndexed { index, row ->
                    menu.add(0, row.id, 2 + index, "    ${row.title}").apply { icon = row.icon; isCheckable = true }
                }
                val preferences = activity.getSharedPreferences("svd_navigation_groups", Activity.MODE_PRIVATE)
                var expanded = preferences.getBoolean("delivery_daily_stock", false)
                fun renderStock() {
                    val builder = menu as? androidx.appcompat.view.menu.MenuBuilder
                    builder?.stopDispatchingItemsChanged()
                    try {
                        header.title = "Daily Stock Entry  ${if (expanded) "\u25B4" else "\u25BE"}"
                        androidx.core.view.MenuItemCompat.setContentDescription(header, "Daily Stock Entry, ${if (expanded) "expanded" else "collapsed"}")
                        children.forEach { it.isVisible = expanded }
                    } finally { builder?.startDispatchingItemsChanged() }
                }
                header.setOnMenuItemClickListener {
                    expanded = !expanded
                    preferences.edit().putBoolean("delivery_daily_stock", expanded).apply()
                    navigation.post { renderStock() }
                    true
                }
                (navigation.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView)?.itemAnimator = null
                renderStock()
            }
            return
        }
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
                header.title = "$title  ${if (expanded) "\u25B4" else "\u25BE"}"
                androidx.core.view.MenuItemCompat.setContentDescription(header, "$title, ${if (expanded) "expanded" else "collapsed"}")
                // Avoid rebuilding the drawer once for each child in this section.
                val menuBuilder = navigation.menu as? androidx.appcompat.view.menu.MenuBuilder
                menuBuilder?.stopDispatchingItemsChanged()
                try {
                    children.forEach { it.isVisible = expanded }
                } finally {
                    menuBuilder?.startDispatchingItemsChanged()
                }
            }
            header.setOnMenuItemClickListener {
                expanded = !expanded
                preferences.edit().putBoolean(title, expanded).apply()
                render()
                true
            }
            render()
        }
        for (entry in standalone.filter { name(it.id) == "nav_logout" }) menu.add(0, entry.id, 0, entry.title).apply { icon = entry.icon }
    }
}
