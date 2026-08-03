package com.svd.svdagencies.ui.admin.stock

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.stock.AdminStockDashboardResponse
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import com.svd.svdagencies.utils.NetworkMessageUtils
import com.svd.svdagencies.utils.showLoading
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminStockUpdateActivity : AdminBaseActivity() {
    
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvUpdateStock: RecyclerView
    private lateinit var btnSubmitUpdate: MaterialButton
    private lateinit var etStockSearch: EditText
    private lateinit var spinnerCompanyFilter: AutoCompleteTextView
    private lateinit var etEntryDate: EditText
    private lateinit var tvUpdateTotalItems: TextView
    private lateinit var tvUpdateEntries: TextView
    private lateinit var tvUpdateDayValue: TextView
    private lateinit var tvNoUpdateCompanyTotals: TextView
    private lateinit var tvNoUpdateSavedEntries: TextView
    private lateinit var rvUpdateCompanyTotals: RecyclerView
    private lateinit var rvUpdateSavedEntries: RecyclerView
    private lateinit var adapter: StockUpdateAdapter
    private lateinit var companyTotalAdapter: StockCompanyTotalAdapter
    private lateinit var savedEntryAdapter: StockDateEntryAdapter
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private var selectedCompany = "All companies"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_stock_update_activity)
        setupAdminLayout("Update Stock")

        swipeRefresh = findViewById(R.id.swipeRefresh)
        rvUpdateStock = findViewById(R.id.rvUpdateStock)
        btnSubmitUpdate = findViewById(R.id.btnSubmitUpdate)
        etStockSearch = findViewById(R.id.etStockSearch)
        spinnerCompanyFilter = findViewById(R.id.spinnerCompanyFilter)
        etEntryDate = findViewById(R.id.etEntryDate)
        tvUpdateTotalItems = findViewById(R.id.tvUpdateTotalItems)
        tvUpdateEntries = findViewById(R.id.tvUpdateEntries)
        tvUpdateDayValue = findViewById(R.id.tvUpdateDayValue)
        tvNoUpdateCompanyTotals = findViewById(R.id.tvNoUpdateCompanyTotals)
        tvNoUpdateSavedEntries = findViewById(R.id.tvNoUpdateSavedEntries)
        rvUpdateCompanyTotals = findViewById(R.id.rvUpdateCompanyTotals)
        rvUpdateSavedEntries = findViewById(R.id.rvUpdateSavedEntries)
        etEntryDate.setText(intent.getStringExtra("ENTRY_DATE") ?: dateFormatter.format(Calendar.getInstance().time))

        adapter = StockUpdateAdapter(emptyList())
        rvUpdateStock.layoutManager = LinearLayoutManager(this)
        rvUpdateStock.adapter = adapter

        companyTotalAdapter = StockCompanyTotalAdapter(emptyList())
        rvUpdateCompanyTotals.layoutManager = LinearLayoutManager(this)
        rvUpdateCompanyTotals.adapter = companyTotalAdapter
        rvUpdateCompanyTotals.isNestedScrollingEnabled = false

        savedEntryAdapter = StockDateEntryAdapter(emptyList())
        rvUpdateSavedEntries.layoutManager = LinearLayoutManager(this)
        rvUpdateSavedEntries.adapter = savedEntryAdapter
        rvUpdateSavedEntries.isNestedScrollingEnabled = false

        swipeRefresh.setOnRefreshListener {
            loadStockItems()
        }

        btnSubmitUpdate.setOnClickListener {
            submitStockUpdates()
        }
        setupFilters()

        loadStockItems()
    }

    private fun setupFilters() {
        etStockSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter.filter(s?.toString().orEmpty(), selectedCompany)
            }
        })

        spinnerCompanyFilter.setText(selectedCompany, false)
        spinnerCompanyFilter.setOnItemClickListener { _, _, _, _ ->
            selectedCompany = spinnerCompanyFilter.text.toString().ifBlank { "All companies" }
            adapter.filter(etStockSearch.text.toString(), selectedCompany)
        }

        etEntryDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        runCatching {
            val parsed = dateFormatter.parse(etEntryDate.text.toString())
            if (parsed != null) calendar.time = parsed
        }
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                etEntryDate.setText(dateFormatter.format(calendar.time))
                loadStockItems()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadStockItems() {
        swipeRefresh.isRefreshing = true
        ApiClient.adminStockApi.getStockDashboard(etEntryDate.text.toString()).enqueue(object : Callback<AdminStockDashboardResponse> {
            override fun onResponse(
                call: Call<AdminStockDashboardResponse>,
                response: Response<AdminStockDashboardResponse>
            ) {
                swipeRefresh.isRefreshing = false
                if (response.isSuccessful && response.body() != null) {
                    val items = response.body()!!.allItems
                    val data = response.body()!!
                    tvUpdateTotalItems.text = "Items: ${data.summary.totalItems}"
                    tvUpdateEntries.text = "Entries: ${data.summary.entriesOnDate}"
                    tvUpdateDayValue.text = String.format(Locale.getDefault(), "Rs. %.2f", data.summary.dayTotalValue)
                    companyTotalAdapter.updateList(data.companyTotals)
                    savedEntryAdapter.updateList(data.dateEntries)
                    tvNoUpdateCompanyTotals.visibility = if (data.companyTotals.isEmpty()) View.VISIBLE else View.GONE
                    rvUpdateCompanyTotals.visibility = if (data.companyTotals.isEmpty()) View.GONE else View.VISIBLE
                    tvNoUpdateSavedEntries.visibility = if (data.dateEntries.isEmpty()) View.VISIBLE else View.GONE
                    rvUpdateSavedEntries.visibility = if (data.dateEntries.isEmpty()) View.GONE else View.VISIBLE
                    adapter.updateList(items)
                    updateCompanyFilter(items.mapNotNull { it.companyName }.distinct().sorted())
                } else {
                    Toast.makeText(this@AdminStockUpdateActivity, "Failed to load items", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AdminStockDashboardResponse>, t: Throwable) {
                swipeRefresh.isRefreshing = false
                Toast.makeText(
                    this@AdminStockUpdateActivity,
                    NetworkMessageUtils.friendlyMessage(t, "Failed to load items"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun updateCompanyFilter(companies: List<String>) {
        val options = listOf("All companies") + companies
        spinnerCompanyFilter.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, options))
        if (selectedCompany !in options) {
            selectedCompany = "All companies"
            spinnerCompanyFilter.setText(selectedCompany, false)
        }
    }

    private fun submitStockUpdates() {
        val updates = adapter.getUpdates()
        if (updates.isEmpty()) {
            Toast.makeText(this, "No changes to update", Toast.LENGTH_SHORT).show()
            return
        }

        btnSubmitUpdate.showLoading(true, "Submitting...")
        showScreenLoading()
        
        val body: Map<String, Any> = mapOf(
            "updates" to updates,
            "entry_date" to etEntryDate.text.toString()
        )

        ApiClient.adminStockApi.updateStock(body).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                btnSubmitUpdate.showLoading(false)
                hideScreenLoading()
                
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminStockUpdateActivity, "Stock updated successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@AdminStockUpdateActivity, AdminStockActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@AdminStockUpdateActivity, "Failed to update stock", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                btnSubmitUpdate.showLoading(false)
                hideScreenLoading()
                
                Toast.makeText(
                    this@AdminStockUpdateActivity,
                    NetworkMessageUtils.friendlyMessage(t, "Failed to update stock"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}
