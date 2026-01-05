package com.svd.svdagencies.ui.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.admin.AdminApi
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.AdminDashboardResponse
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
    private lateinit var tvNotOrdered: TextView
    private lateinit var layoutNoOrdersList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        // Toolbar
        setupAdminLayout("Admin")

        // Views
        swipeRefresh = findViewById(R.id.swipeRefresh)
        tvCustomers = findViewById(R.id.tvCustomers)
        tvItems = findViewById(R.id.tvItems)
        tvSalesToday = findViewById(R.id.tvSalesToday)
        tvDues = findViewById(R.id.tvDues)
        tvPendingOrders = findViewById(R.id.tvPendingOrders)
        tvNotOrdered = findViewById(R.id.tvNotOrdered)
        layoutNoOrdersList = findViewById(R.id.layoutNoOrdersList)

        swipeRefresh.setColorSchemeResources(
            R.color.status_bar,
            R.color.icon_green,
            R.color.icon_orange
        )

        swipeRefresh.setOnRefreshListener {
            loadDashboard()
        }

        swipeRefresh.isRefreshing = true
        loadDashboard()
    }

    private fun loadDashboard() {

        val api = ApiClient.retrofit.create(AdminApi::class.java)

        api.getDashboardCounts().enqueue(object : Callback<AdminDashboardResponse> {

            override fun onResponse(
                call: Call<AdminDashboardResponse>,
                response: Response<AdminDashboardResponse>
            ) {

                swipeRefresh.isRefreshing = false

                if (!response.isSuccessful) {
                    Toast.makeText(
                        this@AdminDashboardActivity,
                        "Server error: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                val data = response.body() ?: return

                // ========= UPDATE DASHBOARD COUNTS =========
                tvCustomers.text = data.customers.toString()
                tvItems.text = data.items.toString()
                tvSalesToday.text = "${data.sales_today}"
                tvDues.text = "${data.dues}"
                tvPendingOrders.text = data.pending_orders.toString()

                // ========= UPDATE NOT ORDERED COUNT =========
                tvNotOrdered.text =
                    data.customers_no_orders_today_count.toString()

                // ========= POPULATE LIST =========
                layoutNoOrdersList.removeAllViews()

                for (customer in data.customers_no_orders_today_list ?: emptyList()) {

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
            }

            override fun onFailure(call: Call<AdminDashboardResponse>, t: Throwable) {
                swipeRefresh.isRefreshing = false
                Toast.makeText(
                    this@AdminDashboardActivity,
                    "Network error: ${t.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}
