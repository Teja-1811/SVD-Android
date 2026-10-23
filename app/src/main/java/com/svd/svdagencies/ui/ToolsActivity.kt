package com.svd.svdagencies.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.svd.svdagencies.R
import com.svd.svdagencies.base.BaseActivity
import com.svd.svdagencies.ui.auth.LoginActivity
import com.svd.svdagencies.utils.Appearance
import com.svd.svdagencies.utils.SessionManager
import com.svd.svdagencies.utils.UserRole

/** Role-filtered native shortcuts and the full website workflows. Website workflows stay inside the app. */
class ToolsActivity : BaseActivity() {
    private data class Tool(val title: String, val section: String, val path: String? = null, val target: Class<*>? = null)
    private lateinit var list: LinearLayout
    private var tools = emptyList<Tool>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = SessionManager(this)
        if (session.getToken().isNullOrBlank()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_tools)
        findViewById<MaterialToolbar>(R.id.toolsToolbar).apply {
            setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            navigationContentDescription = getString(R.string.tools_back)
            setNavigationOnClickListener { finish() }
        }
        val ids = intArrayOf(R.id.paletteRuby, R.id.paletteOcean, R.id.paletteForest)
        findViewById<ChipGroup>(R.id.paletteChoices).apply {
            check(ids[Appearance.selected(this@ToolsActivity)])
            setOnCheckedStateChangeListener { _, checked ->
                val selected = ids.indexOf(checked.firstOrNull() ?: return@setOnCheckedStateChangeListener)
                if (selected >= 0 && selected != Appearance.selected(this@ToolsActivity)) {
                    Appearance.save(this@ToolsActivity, selected)
                    recreate()
                }
            }
        }
        list = findViewById(R.id.toolsList)
        tools = toolsFor(session.getRole())
        val search = findViewById<TextInputEditText>(R.id.toolsSearch)
        search.doAfterTextChanged { render(it?.toString().orEmpty()) }
        render(search.text?.toString().orEmpty())
    }

    private fun render(query: String) {
        list.removeAllViews()
        val matches = tools.filter { (it.title + " " + it.section).contains(query.trim(), ignoreCase = true) }
        if (matches.isEmpty()) {
            list.addView(TextView(this).apply { setText(R.string.tools_empty); setPadding(0, dp(16), 0, dp(16)) })
        }
        matches.groupBy { it.section }.forEach { (section, entries) ->
            list.addView(TextView(this).apply {
                text = section
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
                setPadding(0, dp(18), 0, dp(8))
                androidx.core.view.ViewCompat.setAccessibilityHeading(this, true)
            })
            entries.forEach { tool ->
                list.addView(MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    text = tool.title
                    isAllCaps = false
                    gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
                    minHeight = dp(52)
                    maxLines = 3
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    setOnClickListener { open(tool) }
                })
            }
        }
    }

    private fun open(tool: Tool) {
        try {
            val intent = tool.target?.let { Intent(this, it) }
                ?: Intent(this, WebToolsActivity::class.java)
                    .putExtra(WebToolsActivity.EXTRA_PATH, tool.path.orEmpty())
                    .putExtra(WebToolsActivity.EXTRA_TITLE, tool.title)
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "This tool could not be opened.", Toast.LENGTH_LONG).show()
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun toolsFor(role: String?): List<Tool> {
        val result = mutableListOf<Tool>()
        fun web(title: String, path: String, section: String = "Website tools") { if (role != UserRole.ADMIN) result += Tool(title, section, path) }
        fun native(title: String, target: Class<*>) { result += Tool(title, "App tools", target = target) }
        when (role) {
            UserRole.ADMIN -> {
                native("Dashboard", com.svd.svdagencies.ui.admin.AdminDashboardActivity::class.java)
                native("Customers", com.svd.svdagencies.ui.admin.customer.CustomersData::class.java)
                native("Customer discounts", com.svd.svdagencies.ui.admin.customer.CustomerItemDiscountActivity::class.java)
                native("Routes", com.svd.svdagencies.ui.admin.routes.AdminRoutesActivity::class.java)
                native("Companies", com.svd.svdagencies.ui.admin.companies.AdminCompaniesActivity::class.java)
                native("Items", com.svd.svdagencies.ui.admin.items.AdminItemsActivity::class.java)
                native("Bills", com.svd.svdagencies.ui.admin.bills.AdminBillsActivity::class.java)
                native("Orders", com.svd.svdagencies.ui.admin.AdminOrdersActivity::class.java)
                native("Stock", com.svd.svdagencies.ui.admin.stock.AdminStockActivity::class.java)
                native("Company dues", com.svd.svdagencies.ui.admin.AdminDuesActivity::class.java)
                native("Delivery agent dues", com.svd.svdagencies.ui.admin.AdminDeliveryAgentDuesActivity::class.java)
                native("Cashbook", com.svd.svdagencies.ui.admin.cashbook.AdminCashBookActivity::class.java)
                native("Statement", com.svd.svdagencies.ui.admin.cashbook.AdminStatementActivity::class.java)
                native("Expenses", com.svd.svdagencies.ui.admin.cashbook.ViewExpensesActivity::class.java)
                native("Payments", com.svd.svdagencies.ui.admin.AdminPaymentsActivity::class.java)
                native("Monthly summary", com.svd.svdagencies.ui.admin.AdminMonthlySummary::class.java)
                native("Category sales", com.svd.svdagencies.ui.admin.sales.CategorySalesActivity::class.java)
                native("Offers", com.svd.svdagencies.ui.admin.offers.AdminOffersActivity::class.java)
                native("Subscription offers", com.svd.svdagencies.ui.admin.offers.AdminSubscriptionOffersActivity::class.java)
                native("Order timings", com.svd.svdagencies.ui.admin.AdminOrderTimingsActivity::class.java)
                native("Enquiries", com.svd.svdagencies.ui.admin.AdminEnquiriesActivity::class.java)
                native("GST summary", com.svd.svdagencies.ui.admin.AdminGstActivity::class.java)
                native("Purchase reports", com.svd.svdagencies.ui.admin.AdminPurchaseReportsActivity::class.java)
                native("Users", com.svd.svdagencies.ui.admin.customer.AdminUsersActivity::class.java)
                native("Offer usage analytics", com.svd.svdagencies.ui.admin.AdminOfferUsageActivity::class.java)
                native("Subscriptions report", com.svd.svdagencies.ui.admin.AdminSubscriptionsReportActivity::class.java)
                web("Full administration dashboard", "milk_agency/")
                web("Customer management", "milk_agency/customer-data/")
                web("User management", "milk_agency/user-data/")
                web("Customer discounts", "milk_agency/customer-item-discounts/")
                web("Routes", "milk_agency/routes/")
                web("Companies", "milk_agency/companies/")
                web("Items and brochure", "milk_agency/items/")
                web("Bills and invoices", "milk_agency/bills/")
                web("Create invoice", "milk_agency/generate-bill/")
                web("Orders", "milk_agency/admin-orders-dashboard/")
                web("Order history", "milk_agency/admin-orders-dashboard/history/")
                web("Stock and loss entries", "milk_agency/stock-dashboard/")
                web("Stock entry", "milk_agency/update-stock/")
                web("Customer payment verification and receipts", "milk_agency/customer-payments/")
                web("Company payments and purchase reports", "milk_agency/payments/")
                web("Delivery agent dues", "milk_agency/delivery-agent-dues/")
                web("Cashbook, bank balance and commission credits", "milk_agency/cashbook/")
                web("Expenses", "milk_agency/expenses-list/")
                web("Statement and PDF export", "milk_agency/statement/")
                web("Monthly sales summary and PDF export", "milk_agency/monthly-sales-summary/")
                web("Category sales", "milk_agency/sales-summary-by-category/")
                web("Offer usage analytics", "milk_agency/offers/")
                web("Manage offers", "milk_agency/offers/manage/")
                web("Subscription administration", "milk_agency/subscriptions/")
                web("Manage subscription offers", "milk_agency/subscription-offers/")
                web("Order timings", "milk_agency/order-timings/")
                web("Enquiries", "milk_agency/admin-enquiries/")
                web("GST dashboard", "milk_agency/gst/")
                web("General store GST", "general_store/gst/")
            }
            UserRole.DELIVERY -> {
                native("Create bills", com.svd.svdagencies.ui.delivery.DeliveryBillToCustomerActivity::class.java)
                native("Daily stock entry", com.svd.svdagencies.ui.delivery.DeliveryStockEntryActivity::class.java)
                native("Catalogue", com.svd.svdagencies.ui.delivery.DeliveryCatalogActivity::class.java)
                native("Today report", com.svd.svdagencies.ui.delivery.DeliveryDashboardActivity::class.java)
                native("Subscription deliveries", com.svd.svdagencies.ui.delivery.DeliverySubscriptionDeliveriesActivity::class.java)
                native("Customer payments", com.svd.svdagencies.ui.delivery.DeliveryCustomerPaymentsActivity::class.java)
                native("Bill history", com.svd.svdagencies.ui.delivery.DeliveryBillHistoryActivity::class.java)
                native("Stock history", com.svd.svdagencies.ui.delivery.DeliveryStockHistoryActivity::class.java)
                native("Overall report", com.svd.svdagencies.ui.delivery.DeliveryOverallReportActivity::class.java)
                web("Delivery dashboard", "customer/delivery/")
                web("Create bill", "customer/delivery/create-bill/")
                web("Catalogue", "customer/delivery/catalog/")
                web("Stock entry", "customer/delivery/stock-entry/")
                web("Payments and collections", "customer/delivery/payments/")
                web("Bill history", "customer/delivery/bills/")
                web("Stock history", "customer/delivery/stock-history/")
                web("Overall report", "customer/delivery/report/")
            }
            UserRole.CUSTOMER, UserRole.USER -> {
                native("Home, catalogue, orders and payments", if (role == UserRole.USER) com.svd.svdagencies.ui.customer.UserPortalActivity::class.java else com.svd.svdagencies.ui.customer.CustomerMainActivity::class.java)
                native("Profile", com.svd.svdagencies.ui.customer.CustomerProfileActivity::class.java)
                native("Account statement", com.svd.svdagencies.ui.customer.CustomerStatementActivity::class.java)
                native("Orders and reports", com.svd.svdagencies.ui.customer.CustomerHistoryActivity::class.java)
                native("Subscriptions", com.svd.svdagencies.ui.customer.CustomerSubscriptionsActivity::class.java)
                native("Contact support", com.svd.svdagencies.ui.customer.CustomerContactSupportActivity::class.java)
                native("Raised queries", com.svd.svdagencies.ui.customer.CustomerRaisedQueriesActivity::class.java)
                web("Customer portal", "customer/")
                web("Orders", "customer/orders/")
                web("Place order", "customer/orders/place/")
                web("Order history", "customer/orders/history/")
                web("Reports and invoices", "customer/reports/")
                web("Payments", "customer/collect-payment/")
                web("Update profile", "customer/update-profile/")
            }
        }
        web("Products and company information", "", "Information")
        web("About SVD", "milk_agency/about/", "Information")
        web("Contact", "milk_agency/contact/", "Information")
        web("Privacy policy", "milk_agency/privacy-policy/", "Information")
        web("Terms and conditions", "milk_agency/terms-and-conditions/", "Information")
        web("Refund policy", "milk_agency/refund-policy/", "Information")
        web("Shipment policy", "milk_agency/shipment-policy/", "Information")
        return result
    }
}
