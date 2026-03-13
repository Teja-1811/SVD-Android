package com.svd.svdagencies.ui.admin.stock

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.stock.AdminStockDashboardResponse
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import com.svd.svdagencies.utils.NetworkMessageUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class AdminStockActivity : AdminBaseActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvTotalItems: TextView
    private lateinit var tvTotalValue: TextView
    private lateinit var tvLowStock: TextView
    private lateinit var tvMovementIn: TextView
    private lateinit var tvMovementOut: TextView
    
    private lateinit var barChart: BarChart
    private lateinit var pieChart: PieChart
    private lateinit var rvStockOverview: RecyclerView
    private lateinit var adapter: StockOverviewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_stock_dashboard)

        setupAdminLayout("Stock")

        // Initialize Views
        swipeRefresh = findViewById(R.id.swipeRefresh)
        tvTotalItems = findViewById(R.id.tvTotalItems)
        tvTotalValue = findViewById(R.id.tvTotalValue)
        tvLowStock = findViewById(R.id.tvLowStock)
        tvMovementIn = findViewById(R.id.tvMovementIn)
        tvMovementOut = findViewById(R.id.tvMovementOut)
        
        barChart = findViewById(R.id.barChart)
        pieChart = findViewById(R.id.pieChart)
        rvStockOverview = findViewById(R.id.rvStockOverview)

        // Setup RecyclerView
        adapter = StockOverviewAdapter(emptyList())
        rvStockOverview.layoutManager = LinearLayoutManager(this)
        rvStockOverview.adapter = adapter

        swipeRefresh.setOnRefreshListener {
            loadStockData()
        }

        loadStockData()
    }

    private fun loadStockData() {
        swipeRefresh.isRefreshing = true

        ApiClient.adminStockApi.getStockDashboard().enqueue(object : Callback<AdminStockDashboardResponse> {
            override fun onResponse(
                call: Call<AdminStockDashboardResponse>,
                response: Response<AdminStockDashboardResponse>
            ) {
                swipeRefresh.isRefreshing = false
                if (response.isSuccessful && response.body() != null) {
                    updateUI(response.body()!!)
                } else {
                    Toast.makeText(this@AdminStockActivity, "Failed to load data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AdminStockDashboardResponse>, t: Throwable) {
                swipeRefresh.isRefreshing = false
                Toast.makeText(
                    this@AdminStockActivity,
                    NetworkMessageUtils.friendlyMessage(t, "Failed to load stock"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun updateUI(data: AdminStockDashboardResponse) {
        // Update Summary Cards
        tvTotalItems.text = data.summary.totalItems.toString()
        tvTotalValue.text = String.format(Locale.getDefault(), "₹%.2f", data.summary.totalStockValue)
        tvLowStock.text = data.summary.lowStockCount.toString()
        tvMovementIn.text = String.format(Locale.getDefault(), "In: %.0f", data.summary.stockIn30d)
        tvMovementOut.text = String.format(Locale.getDefault(), "Out: %.0f", data.summary.stockOut30d)

        // Update Charts
        setupBarChart(data.topItems)
        setupPieChart(data.companyData)

        // Update Table
        adapter.updateList(data.allItems)
    }

    private fun setupBarChart(topItems: List<com.svd.svdagencies.data.model.admin.stock.StockItem>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        topItems.forEachIndexed { index, item ->
            entries.add(BarEntry(index.toFloat(), (item.stockValue ?: 0.0).toFloat()))
            labels.add(item.name.take(10) + "...") // Shorten names for display
        }

        val dataSet = BarDataSet(entries, "Stock Value")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        
        val data = BarData(dataSet)
        barChart.data = data
        
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.setDrawGridLines(false)
        barChart.xAxis.granularity = 1f
        barChart.xAxis.isGranularityEnabled = true
        
        barChart.description.isEnabled = false
        barChart.animateY(1000)
        barChart.invalidate()
    }

    private fun setupPieChart(companyData: List<com.svd.svdagencies.data.model.admin.stock.CompanyStockValue>) {
        val entries = ArrayList<PieEntry>()

        companyData.forEach {
            entries.add(PieEntry(it.totalValue.toFloat(), it.companyName))
        }

        val dataSet = PieDataSet(entries, "Company Stock")
        dataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()
        dataSet.sliceSpace = 3f
        
        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.centerText = "Stock Value"
        pieChart.animateXY(1000, 1000)
        pieChart.invalidate()
    }
}
