package com.svd.svdagencies.ui.admin

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.SaveDailyPaymentsRequest
import com.svd.svdagencies.ui.admin.adapter.CompanyPaymentsAdapter
import com.svd.svdagencies.utils.NetworkMessageUtils
import kotlinx.coroutines.launch
import java.util.Calendar

class AdminDuesActivity : AdminBaseActivity() {

    private lateinit var tvDate: TextView
    private lateinit var tvTotalInvoice: TextView
    private lateinit var tvTotalPaid: TextView
    private lateinit var tvTotalDue: TextView
    private lateinit var btnSaveAll: MaterialButton
    private lateinit var rvSummary: RecyclerView
    private lateinit var layoutIndicators: LinearLayout
    
    private lateinit var adapter: CompanyPaymentsAdapter

    private var currentYear: Int = 0
    private var currentMonth: Int = 0
    private var allCompanyPayments: List<com.svd.svdagencies.data.model.admin.CompanyPayment> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_companies_due)

        setupAdminLayout("Company Payments")

        val c = Calendar.getInstance()
        currentYear = c.get(Calendar.YEAR)
        currentMonth = c.get(Calendar.MONTH) + 1

        tvDate = findViewById(R.id.tvDate)
        tvTotalInvoice = findViewById(R.id.tvTotalInvoice)
        tvTotalPaid = findViewById(R.id.tvTotalPaid)
        tvTotalDue = findViewById(R.id.tvTotalDue)
        btnSaveAll = findViewById(R.id.btnSaveAll)
        rvSummary = findViewById(R.id.rvSummary)
        layoutIndicators = findViewById(R.id.layoutIndicators)

        // Indicators are not needed for vertical view
        layoutIndicators.visibility = View.GONE

        updateDateLabel()
        setupRecycler()
        setupListeners()
        loadPaymentsDashboard(currentYear, currentMonth)
    }

    private fun setupRecycler() {
        adapter = CompanyPaymentsAdapter(emptyList())
        // Change to Vertical Orientation
        rvSummary.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rvSummary.adapter = adapter
        
        // Prevent focus from jumping when items are recycled
        rvSummary.descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
        
        // PagerSnapHelper removed as it's not needed for vertical list
    }

    private fun updateDateLabel() {
        val monthNames = arrayOf("January", "February", "March", "April", "May", "June", 
            "July", "August", "September", "October", "November", "December")
        tvDate.text = "${monthNames[currentMonth - 1]}, $currentYear"
    }

    private fun setupListeners() {
        tvDate.setOnClickListener { showMonthYearPicker() }
        btnSaveAll.setOnClickListener { saveAllChanges() }
    }

    private fun showMonthYearPicker() {
        DatePickerDialog(this, { _, year, month, _ ->
            currentYear = year
            currentMonth = month + 1
            updateDateLabel()
            loadPaymentsDashboard(currentYear, currentMonth)
        }, currentYear, currentMonth - 1, 1).show()
    }

    private fun loadPaymentsDashboard(year: Int, month: Int) {
        lifecycleScope.launch {
            showScreenLoading()
            try {
                val response = ApiClient.adminPaymentsApi.getPaymentsDashboard(year, month)
                tvTotalInvoice.text = "₹${response.grand_total_invoice}"
                tvTotalPaid.text = "₹${response.grand_total_paid}"
                tvTotalDue.text = "₹${response.grand_total_due}"
                
                allCompanyPayments = response.payments
                adapter.updateList(allCompanyPayments)

            } catch (e: Exception) {
                Toast.makeText(
                    this@AdminDuesActivity,
                    NetworkMessageUtils.friendlyMessage(e, "Failed to load payments"),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                btnSaveAll.isEnabled = true
                hideScreenLoading()
            }
        }
    }

    private fun saveAllChanges() {
        val dataMap = adapter.getPaymentDataWithContext(currentYear, currentMonth)
        if (dataMap.isEmpty()) return

        btnSaveAll.isEnabled = false
        showScreenLoading()
        lifecycleScope.launch {
            try {
                ApiClient.adminPaymentsApi.saveDailyPayments(SaveDailyPaymentsRequest(currentYear, currentMonth, dataMap))
                Toast.makeText(this@AdminDuesActivity, "Saved Successfully!", Toast.LENGTH_SHORT).show()
                loadPaymentsDashboard(currentYear, currentMonth)
            } catch (e: Exception) {
                btnSaveAll.isEnabled = true
                hideScreenLoading()
                Toast.makeText(
                    this@AdminDuesActivity,
                    NetworkMessageUtils.friendlyMessage(e, "Failed to save payments"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
