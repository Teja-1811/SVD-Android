package com.svd.svdagencies.ui.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.admin.AdminApi
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.AdminDashboardResponse
import com.svd.svdagencies.ui.admin.customer.CustomersData
import com.svd.svdagencies.ui.admin.cashbook.AdminStatementActivity
import com.svd.svdagencies.ui.admin.items.AdminItemsActivity
import com.svd.svdagencies.ui.admin.stock.AdminStockUpdateActivity
import com.svd.svdagencies.utils.RefreshManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminDashboardActivity : AdminBaseActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvCustomers: TextView
    private lateinit var tvItems: TextView
    private lateinit var tvSalesToday: TextView
    private lateinit var tvDues: TextView
    private lateinit var tvPendingOrders: TextView
    private lateinit var tvEnquiries: TextView
    private lateinit var tvNotOrdered: TextView
    private lateinit var layoutNoOrdersList: LinearLayout
    private lateinit var layoutTopDuesList: LinearLayout
    private lateinit var layoutTopStockList: LinearLayout
    private lateinit var btnViewOrders: ImageButton
    private lateinit var btnViewEnquiries: ImageButton
    private lateinit var btnUpdateStock: Button
    private lateinit var btnStatement: Button

    private lateinit var cardCustomers: View
    private lateinit var cardItems: View
    private lateinit var cardSales: View
    private lateinit var cardDues: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin)

        // Toolbar
        setupAdminLayout("Admin")

        // Views
        swipeRefresh = findViewById(R.id.swipeRefresh)
        tvPendingOrders = findViewById(R.id.tvPendingOrders)
        tvEnquiries = findViewById(R.id.tvEnquiries)
        tvNotOrdered = findViewById(R.id.tvNotOrdered)
        layoutNoOrdersList = findViewById(R.id.layoutNoOrdersList)
        layoutTopDuesList = findViewById(R.id.layoutTopDuesList)
        layoutTopStockList = findViewById(R.id.layoutTopStockList)
        btnViewOrders = findViewById(R.id.btnViewOrders)
        btnViewEnquiries = findViewById(R.id.btnViewEnquiries)
        btnUpdateStock = findViewById(R.id.btnUpdateStock)
        btnStatement = findViewById(R.id.btnStatement)

        cardCustomers = findViewById(R.id.cardCustomers)
        cardItems = findViewById(R.id.cardItems)
        cardSales = findViewById(R.id.cardSales)
        cardDues = findViewById(R.id.cardDues)

        // Setup Stat Cards
        setupStatCard(cardCustomers, "Customers", R.drawable.ic_users, R.color.brand_red)
        setupStatCard(cardItems, "Items", R.drawable.ic_items, R.color.icon_green)
        setupStatCard(cardSales, "Sales Today", R.drawable.ic_rupee, R.color.icon_orange)
        setupStatCard(cardDues, "Dues", R.drawable.ic_bill, R.color.brand_red)

        tvCustomers = cardCustomers.findViewById(R.id.tvStatValue)
        tvItems = cardItems.findViewById(R.id.tvStatValue)
        tvSalesToday = cardSales.findViewById(R.id.tvStatValue)
        tvDues = cardDues.findViewById(R.id.tvStatValue)

        btnViewOrders.setOnClickListener {
            val intent = Intent(this, AdminOrdersActivity::class.java)
            startActivity(intent)
        }

        btnViewEnquiries.setOnClickListener {
            val intent = Intent(this, AdminEnquiriesActivity::class.java)
            startActivity(intent)
        }

        cardCustomers.setOnClickListener {
            startActivity(Intent(this, CustomersData::class.java))
        }

        cardItems.setOnClickListener {
            startActivity(Intent(this, AdminItemsActivity::class.java))
        }

        btnUpdateStock.setOnClickListener {
            startActivity(Intent(this, AdminStockUpdateActivity::class.java))
        }

        btnStatement.setOnClickListener {
            startActivity(Intent(this, AdminStatementActivity::class.java))
        }

        // Use RefreshManager to setup swipe refresh
        RefreshManager.setupRefresh(swipeRefresh) {
            loadDashboard()
        }

        RefreshManager.startRefresh(swipeRefresh)
        loadDashboard()
    }

    private fun setupStatCard(card: View, label: String, iconRes: Int, colorRes: Int) {
        card.findViewById<TextView>(R.id.tvStatLabel).text = label
        val iconView = card.findViewById<ImageView>(R.id.ivStatIcon)
        iconView.setImageResource(iconRes)
        iconView.setColorFilter(ContextCompat.getColor(this, colorRes))
    }

    private fun loadDashboard() {

        val api = ApiClient.retrofit.create(AdminApi::class.java)

        api.getDashboardCounts().enqueue(object : Callback<AdminDashboardResponse> {

            override fun onResponse(
                call: Call<AdminDashboardResponse>,
                response: Response<AdminDashboardResponse>
            ) {
                if (isDestroyed) return
                RefreshManager.stopRefresh(swipeRefresh)

                if (!response.isSuccessful) {
                    Toast.makeText(
                        this@AdminDashboardActivity,
                        "Server error: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                val data = response.body() ?: return
                val summary = data.summary

                // ========= UPDATE DASHBOARD COUNTS =========
                tvCustomers.text = (summary?.customers ?: 0).toString()
                tvItems.text = (summary?.items ?: 0).toString()
                tvSalesToday.text = "₹${summary?.sales_today ?: 0.0}"
                tvDues.text = "₹${summary?.dues ?: 0.0}"
                tvPendingOrders.text = (summary?.pending_orders ?: 0).toString()
                tvEnquiries.text = (summary?.active_enquiries ?: 0).toString()

                // ========= UPDATE NOT ORDERED COUNT =========
                tvNotOrdered.text =
                    (summary?.customers_no_orders_today_count ?: 0).toString()

                // ========= POPULATE LIST =========
                layoutNoOrdersList.removeAllViews()

                val customers = data.customers_no_orders_today_list ?: emptyList()

                for (customer in customers.take(20)) { // Limit to top 20 for safety

                    val row = LayoutInflater.from(this@AdminDashboardActivity)
                        .inflate(
                            R.layout.admin_no_order_customer,
                            layoutNoOrdersList,
                            false
                        )

                    val txtName =
                        row.findViewById<TextView>(R.id.txtCustomerName)
                    val btnCall =
                        row.findViewById<ImageView>(R.id.btnCall)

                    txtName.text = customer.name

                    btnCall.setOnClickListener {
                        val intent = Intent(Intent.ACTION_DIAL)
                        intent.data = Uri.parse("tel:${customer.phone}")
                        startActivity(intent)
                    }

                    layoutNoOrdersList.addView(row)
                }

                // ========= POPULATE TOP DUES =========
                layoutTopDuesList.removeAllViews()
                data.top_due_customers?.forEach { customer ->
                    val row = LayoutInflater.from(this@AdminDashboardActivity)
                        .inflate(R.layout.admin_top_due_customer_item, layoutTopDuesList, false)
                    
                    row.findViewById<TextView>(R.id.txtCustomerName).text = customer.name
                    row.findViewById<TextView>(R.id.txtCustomerPhone).text = customer.phone
                    row.findViewById<TextView>(R.id.txtDueAmount).text = "₹${customer.actual_due}"
                    
                    layoutTopDuesList.addView(row)
                }

                // ========= POPULATE TOP STOCK =========
                layoutTopStockList.removeAllViews()
                data.top_stock_items?.forEach { item ->
                    val row = LayoutInflater.from(this@AdminDashboardActivity)
                        .inflate(R.layout.admin_top_stock_item, layoutTopStockList, false)
                    
                    row.findViewById<TextView>(R.id.txtItemName).text = item.name
                    row.findViewById<TextView>(R.id.txtItemCategory).text = item.category
                    row.findViewById<TextView>(R.id.txtStockQty).text = "${item.stock_quantity} pcs"
                    
                    layoutTopStockList.addView(row)
                }
            }

            override fun onFailure(call: Call<AdminDashboardResponse>, t: Throwable) {
                if (isDestroyed) return
                RefreshManager.stopRefresh(swipeRefresh)
                Toast.makeText(
                    this@AdminDashboardActivity,
                    "Network error: ${t.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}
