package com.svd.svdagencies.ui.admin

import android.content.Intent
import androidx.core.net.toUri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.admin.AdminApi
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.AdminDashboardResponse
import com.svd.svdagencies.databinding.AdminBinding
import com.svd.svdagencies.databinding.AdminEnquiryPreviewItemBinding
import com.svd.svdagencies.databinding.AdminNoOrderCustomerBinding
import com.svd.svdagencies.databinding.AdminOfferPreviewItemBinding
import com.svd.svdagencies.databinding.AdminPendingOrderPreviewItemBinding
import com.svd.svdagencies.databinding.AdminTopDueCustomerItemBinding
import com.svd.svdagencies.databinding.AdminTopSellingItemBinding
import com.svd.svdagencies.databinding.AdminTopStockItemBinding
import com.svd.svdagencies.ui.admin.customer.CustomersData
import com.svd.svdagencies.ui.admin.cashbook.AdminStatementActivity
import com.svd.svdagencies.ui.admin.items.AdminItemsActivity
import com.svd.svdagencies.ui.admin.stock.AdminStockUpdateActivity
import com.svd.svdagencies.utils.RefreshManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminDashboardActivity : AdminBaseActivity() {

    private lateinit var binding: AdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar
        setupAdminLayout(getString(R.string.title_dashboard))

        // Setup Stat Cards
        setupStatCard(binding.cardCustomers.root, getString(R.string.label_customers), R.drawable.ic_users, R.color.brand_red)
        setupStatCard(binding.cardItems.root, getString(R.string.label_items), R.drawable.ic_items, R.color.icon_green)
        setupStatCard(binding.cardSales.root, getString(R.string.label_sales_today), R.drawable.ic_rupee, R.color.icon_orange)
        setupStatCard(binding.cardDues.root, getString(R.string.label_dues), R.drawable.ic_bill, R.color.brand_red)
        setupStatCard(binding.cardProfit.root, "Profit Today", R.drawable.ic_rupee, R.color.icon_green)
        setupStatCard(binding.cardStockValue.root, "Stock Value", R.drawable.ic_stock, R.color.icon_blue)

        binding.btnViewOrders.setOnClickListener {
            val intent = Intent(this, AdminOrdersActivity::class.java)
            startActivity(intent)
        }

        binding.btnViewEnquiries.setOnClickListener {
            val intent = Intent(this, AdminEnquiriesActivity::class.java)
            startActivity(intent)
        }

        binding.cardCustomers.root.setOnClickListener {
            startActivity(Intent(this, CustomersData::class.java))
        }

        binding.cardItems.root.setOnClickListener {
            startActivity(Intent(this, AdminItemsActivity::class.java))
        }

        binding.btnUpdateStock.setOnClickListener {
            startActivity(Intent(this, AdminStockUpdateActivity::class.java))
        }

        binding.btnStatement.setOnClickListener {
            startActivity(Intent(this, AdminStatementActivity::class.java))
        }

        // Use RefreshManager to setup swipe refresh
        RefreshManager.setupRefresh(binding.swipeRefresh) {
            loadDashboard()
        }

        RefreshManager.startRefresh(binding.swipeRefresh)
    }

    override fun onResume() {
        super.onResume()
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
                RefreshManager.stopRefresh(binding.swipeRefresh)

                if (!response.isSuccessful) {
                    Toast.makeText(
                        this@AdminDashboardActivity,
                        getString(R.string.server_error_code, response.code()),
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                val data = response.body() ?: return
                val summary = data.summary

                // ========= UPDATE DASHBOARD COUNTS =========
                binding.cardCustomers.tvStatValue.text = (summary?.customers ?: 0).toString()
                binding.cardItems.tvStatValue.text = (summary?.items ?: 0).toString()
                binding.cardSales.tvStatValue.text = getString(R.string.format_currency_symbol, summary?.sales_today ?: 0.0)
                binding.cardDues.tvStatValue.text = getString(R.string.format_currency_symbol, summary?.dues ?: 0.0)
                binding.cardProfit.tvStatValue.text = getString(R.string.format_currency_symbol, summary?.profit_today ?: 0.0)
                binding.cardStockValue.tvStatValue.text = getString(R.string.format_currency_symbol, summary?.stock_value ?: 0.0)
                
                binding.tvPendingOrders.text = (summary?.pending_orders ?: 0).toString()
                binding.tvEnquiries.text = (summary?.active_enquiries ?: 0).toString()

                // ========= PENDING ORDERS PREVIEW =========
                binding.layoutPendingOrdersList.removeAllViews()
                val pendingOrders = data.pending_orders_preview ?: emptyList()
                if (pendingOrders.isNotEmpty()) {
                    binding.layoutPendingOrdersList.visibility = View.VISIBLE
                    pendingOrders.forEach { order ->
                        val row = AdminPendingOrderPreviewItemBinding.inflate(layoutInflater, binding.layoutPendingOrdersList, false)
                        row.txtOrderNumber.text = order.order_number
                        row.txtCustomerName.text = order.customer_name
                        row.txtOrderAmount.text = getString(R.string.format_currency_symbol, order.approved_total_amount)
                        binding.layoutPendingOrdersList.addView(row.root)
                    }
                } else {
                    binding.layoutPendingOrdersList.visibility = View.GONE
                }

                // ========= ENQUIRIES PREVIEW =========
                binding.layoutEnquiriesList.removeAllViews()
                val enquiries = data.active_enquiries_preview ?: emptyList()
                if (enquiries.isNotEmpty()) {
                    binding.layoutEnquiriesList.visibility = View.VISIBLE
                    enquiries.forEach { enquiry ->
                        val row = AdminEnquiryPreviewItemBinding.inflate(layoutInflater, binding.layoutEnquiriesList, false)
                        row.txtSubject.text = enquiry.subject
                        row.txtName.text = enquiry.name
                        binding.layoutEnquiriesList.addView(row.root)
                    }
                } else {
                    binding.layoutEnquiriesList.visibility = View.GONE
                }

                // ========= UPDATE NOT ORDERED COUNT =========
                binding.tvNotOrdered.text =
                    (summary?.customers_no_orders_today_count ?: 0).toString()

                // ========= POPULATE LIST =========
                binding.layoutNoOrdersList.removeAllViews()

                val customers = data.customers_no_orders_today_list ?: emptyList()

                for (customer in customers.take(20)) { // Limit to top 20 for safety

                    val rowBinding = AdminNoOrderCustomerBinding.inflate(
                        LayoutInflater.from(this@AdminDashboardActivity),
                        binding.layoutNoOrdersList,
                        false
                    )

                    rowBinding.txtCustomerName.text = customer.name

                    rowBinding.btnCall.setOnClickListener {
                        val intent = Intent(Intent.ACTION_DIAL)
                        intent.data = "tel:${customer.phone}".toUri()
                        startActivity(intent)
                    }

                    binding.layoutNoOrdersList.addView(rowBinding.root)
                }

                // ========= TOP SELLING ITEMS =========
                binding.layoutTopSellingList.removeAllViews()
                data.top_selling_items_today?.forEach { item ->
                    val row = AdminTopSellingItemBinding.inflate(layoutInflater, binding.layoutTopSellingList, false)
                    row.txtItemName.text = item.item_name
                    row.txtQuantity.text = "Qty: ${item.quantity}"
                    row.txtAmount.text = getString(R.string.format_currency_symbol, item.amount)
                    binding.layoutTopSellingList.addView(row.root)
                }

                // ========= ACTIVE OFFERS =========
                binding.layoutActiveOffersList.removeAllViews()
                data.active_offers_preview?.forEach { offer ->
                    val row = AdminOfferPreviewItemBinding.inflate(layoutInflater, binding.layoutActiveOffersList, false)
                    row.txtOfferName.text = offer.name
                    row.txtOfferDetail.text = "${offer.offer_for} | ${offer.offer_type}"
                    row.txtEndDate.text = "Ends: ${offer.end_date}"
                    binding.layoutActiveOffersList.addView(row.root)
                }

                // ========= POPULATE TOP DUES =========
                binding.layoutTopDuesList.removeAllViews()
                data.top_due_customers?.forEach { customer ->
                    val rowBinding = AdminTopDueCustomerItemBinding.inflate(
                        LayoutInflater.from(this@AdminDashboardActivity),
                        binding.layoutTopDuesList,
                        false
                    )
                    
                    rowBinding.txtCustomerName.text = customer.name
                    rowBinding.txtCustomerPhone.text = customer.phone
                    rowBinding.txtDueAmount.text = getString(R.string.format_currency_symbol, customer.actual_due)
                    
                    binding.layoutTopDuesList.addView(rowBinding.root)
                }

                // ========= POPULATE TOP STOCK =========
                binding.layoutTopStockList.removeAllViews()
                data.top_stock_items?.forEach { item ->
                    val rowBinding = AdminTopStockItemBinding.inflate(
                        LayoutInflater.from(this@AdminDashboardActivity),
                        binding.layoutTopStockList,
                        false
                    )
                    
                    rowBinding.txtItemName.text = item.name
                    rowBinding.txtItemCategory.text = item.category
                    rowBinding.txtStockQty.text = getString(R.string.format_pcs, item.stock_quantity ?: 0)
                    
                    binding.layoutTopStockList.addView(rowBinding.root)
                }
            }

            override fun onFailure(call: Call<AdminDashboardResponse>, t: Throwable) {
                if (isDestroyed) return
                RefreshManager.stopRefresh(binding.swipeRefresh)
                Toast.makeText(
                    this@AdminDashboardActivity,
                    getString(R.string.network_error_message, t.localizedMessage),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}
