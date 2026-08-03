package com.svd.svdagencies.ui.admin.stock

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
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
import com.svd.svdagencies.data.model.admin.stock.StockDateEntry
import com.svd.svdagencies.data.model.admin.stock.StockLeakageEntry
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.view.LayoutInflater
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class AdminStockActivity : AdminBaseActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvTotalItems: TextView
    private lateinit var tvTotalValue: TextView
    private lateinit var tvLowStock: TextView
    private lateinit var tvMovementIn: TextView
    private lateinit var tvMovementOut: TextView
    private lateinit var tvMonthlyLeakage: TextView
    private lateinit var tvDateRecordsSubtitle: TextView
    private lateinit var tvNoDateRecords: TextView
    private lateinit var tvNoLeakage: TextView
    private lateinit var etRecordDate: EditText
    private lateinit var btnManageStock: MaterialButton
    
    private lateinit var barChart: BarChart
    private lateinit var pieChart: PieChart
    private lateinit var rvStockOverview: RecyclerView
    private lateinit var rvDateEntries: RecyclerView
    private lateinit var rvLeakageEntries: RecyclerView
    private lateinit var adapter: StockOverviewAdapter
    private lateinit var dateEntryAdapter: StockDateEntryAdapter
    private lateinit var leakageAdapter: StockLeakageAdapter
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private var selectedDate = dateFormatter.format(Calendar.getInstance().time)

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
        tvMonthlyLeakage = findViewById(R.id.tvMonthlyLeakage)
        tvDateRecordsSubtitle = findViewById(R.id.tvDateRecordsSubtitle)
        tvNoDateRecords = findViewById(R.id.tvNoDateRecords)
        tvNoLeakage = findViewById(R.id.tvNoLeakage)
        etRecordDate = findViewById(R.id.etRecordDate)
        btnManageStock = findViewById(R.id.btnManageStock)
        
        barChart = findViewById(R.id.barChart)
        pieChart = findViewById(R.id.pieChart)
        rvStockOverview = findViewById(R.id.rvStockOverview)
        rvDateEntries = findViewById(R.id.rvDateEntries)
        rvLeakageEntries = findViewById(R.id.rvLeakageEntries)
        etRecordDate.setText(selectedDate)

        // Setup RecyclerView
        adapter = StockOverviewAdapter(emptyList())
        rvStockOverview.layoutManager = LinearLayoutManager(this)
        rvStockOverview.adapter = adapter
        rvStockOverview.isNestedScrollingEnabled = false

        dateEntryAdapter = StockDateEntryAdapter(
            entries = emptyList(),
            onEdit = { entry -> showEditEntryDialog(entry) },
            onDelete = { entry -> showDeleteEntryConfirm(entry) }
        )
        rvDateEntries.layoutManager = LinearLayoutManager(this)
        rvDateEntries.adapter = dateEntryAdapter
        rvDateEntries.isNestedScrollingEnabled = false

        leakageAdapter = StockLeakageAdapter(
            entries = emptyList(),
            onDelete = { entry -> showDeleteLeakageConfirm(entry) }
        )
        rvLeakageEntries.layoutManager = LinearLayoutManager(this)
        rvLeakageEntries.adapter = leakageAdapter
        rvLeakageEntries.isNestedScrollingEnabled = false

        etRecordDate.setOnClickListener {
            showDatePicker()
        }
        btnManageStock.setOnClickListener {
            startActivity(
                Intent(this, AdminStockUpdateActivity::class.java)
                    .putExtra("ENTRY_DATE", selectedDate)
            )
        }

        swipeRefresh.setOnRefreshListener {
            loadStockData()
        }
    }

    override fun onResume() {
        super.onResume()
        loadStockData()
    }

    private fun loadStockData() {
        swipeRefresh.isRefreshing = true

        val calendar = Calendar.getInstance()
        runCatching {
            val parsed = dateFormatter.parse(selectedDate)
            if (parsed != null) calendar.time = parsed
        }

        ApiClient.adminStockApi.getStockDashboard(
            selectedDate,
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.YEAR)
        ).enqueue(object : Callback<AdminStockDashboardResponse> {
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

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        runCatching {
            val parsed = dateFormatter.parse(selectedDate)
            if (parsed != null) calendar.time = parsed
        }
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDate = dateFormatter.format(calendar.time)
                etRecordDate.setText(selectedDate)
                loadStockData()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateUI(data: AdminStockDashboardResponse) {
        // Update Summary Cards
        tvTotalItems.text = data.summary.totalItems.toString()
        tvTotalValue.text = String.format(Locale.getDefault(), "₹%.2f", data.summary.totalStockValue)
        tvLowStock.text = data.summary.lowStockCount.toString()
        tvMovementIn.text = String.format(Locale.getDefault(), "In: %.0f", data.summary.stockIn30d)
        tvMovementOut.text = String.format(Locale.getDefault(), "Out: %.0f", data.summary.stockOut30d)
        tvMonthlyLeakage.text = String.format(Locale.getDefault(), "Rs. %.2f", data.summary.monthlyLoss)
        selectedDate = data.selectedDate ?: selectedDate
        etRecordDate.setText(selectedDate)
        tvDateRecordsSubtitle.text = "Saved crates for $selectedDate with edit support for the same date."

        dateEntryAdapter.updateList(data.dateEntries)
        tvNoDateRecords.visibility = if (data.dateEntries.isEmpty()) View.VISIBLE else View.GONE
        rvDateEntries.visibility = if (data.dateEntries.isEmpty()) View.GONE else View.VISIBLE

        leakageAdapter.updateList(data.leakageEntries)
        tvNoLeakage.visibility = if (data.leakageEntries.isEmpty()) View.VISIBLE else View.GONE
        rvLeakageEntries.visibility = if (data.leakageEntries.isEmpty()) View.GONE else View.VISIBLE

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

    private fun showEditEntryDialog(entry: StockDateEntry) {
        val view = LayoutInflater.from(this).inflate(R.layout.delivery_edit_stock_entry, null)
        val etCrates = view.findViewById<TextInputEditText>(R.id.etCrates)
        val etDiscount = view.findViewById<TextInputEditText>(R.id.etDiscount)

        etCrates.setText(entry.crates.toString())
        etDiscount.setText("0") // Backend might not return previous discount, setting 0 as default

        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Stock Entry")
            .setMessage("Updating ${entry.itemName} for ${entry.companyName}")
            .setView(view)
            .setPositiveButton("Update") { _, _ ->
                val crates = etCrates.text.toString().toDoubleOrNull() ?: 0.0
                val discount = etDiscount.text.toString().toDoubleOrNull() ?: 0.0
                if (crates > 0) {
                    updateStockEntry(entry.id, crates, discount)
                } else {
                    Toast.makeText(this, "Enter valid crates", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateStockEntry(id: Int, crates: Double, discount: Double) {
        showScreenLoading()
        val body = mapOf("crates" to crates, "discount" to discount)
        ApiClient.adminStockApi.editStockEntry(id, body).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                hideScreenLoading()
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminStockActivity, "Updated successfully", Toast.LENGTH_SHORT).show()
                    loadStockData()
                } else {
                    Toast.makeText(this@AdminStockActivity, "Update failed", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                hideScreenLoading()
                Toast.makeText(this@AdminStockActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showDeleteEntryConfirm(entry: StockDateEntry) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Stock Entry")
            .setMessage("Are you sure you want to delete this entry for ${entry.itemName}? This will restore the stock levels.")
            .setPositiveButton("Delete") { _, _ -> deleteStockEntry(entry.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteStockEntry(id: Int) {
        showScreenLoading()
        ApiClient.adminStockApi.deleteStockEntry(id).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                hideScreenLoading()
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminStockActivity, "Deleted successfully", Toast.LENGTH_SHORT).show()
                    loadStockData()
                } else {
                    Toast.makeText(this@AdminStockActivity, "Delete failed", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                hideScreenLoading()
                Toast.makeText(this@AdminStockActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showDeleteLeakageConfirm(entry: StockLeakageEntry) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Leakage Record")
            .setMessage("Restore ${entry.quantity} units to ${entry.itemName} stock?")
            .setPositiveButton("Delete") { _, _ -> deleteLeakage(entry.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteLeakage(id: Int) {
        showScreenLoading()
        ApiClient.adminStockApi.deleteLeakage(id).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                hideScreenLoading()
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminStockActivity, "Leakage deleted", Toast.LENGTH_SHORT).show()
                    loadStockData()
                }
            }
            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                hideScreenLoading()
            }
        })
    }
}
