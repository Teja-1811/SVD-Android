package com.svd.svdagencies.ui.admin

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.SaveDailyPaymentsRequest
import com.svd.svdagencies.ui.admin.adapter.CompanyPaymentsAdapter
import kotlinx.coroutines.launch
import java.util.Calendar

class AdminDuesActivity : AdminBaseActivity() {

    private lateinit var tvDate: TextView
    
    private lateinit var tvTotalInvoice: TextView
    private lateinit var tvTotalPaid: TextView
    private lateinit var tvTotalDue: TextView
    private lateinit var btnSaveAll: MaterialButton
    private lateinit var rvSummary: RecyclerView
    
    private lateinit var adapter: CompanyPaymentsAdapter

    // Data handling
    private var currentYear: Int = 0
    private var currentMonth: Int = 0
    
    // We will store the dashboard data here. 
    private var allCompanyPayments: List<com.svd.svdagencies.data.model.admin.CompanyPayment> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_companies_due)

        setupAdminLayout("Company Payments")

        // Initialize current date
        val c = Calendar.getInstance()
        currentYear = c.get(Calendar.YEAR)
        currentMonth = c.get(Calendar.MONTH) + 1 // 1-based

        // Views
        tvDate = findViewById(R.id.tvDate)

        tvTotalInvoice = findViewById(R.id.tvTotalInvoice)
        tvTotalPaid = findViewById(R.id.tvTotalPaid)
        tvTotalDue = findViewById(R.id.tvTotalDue)
        btnSaveAll = findViewById(R.id.btnSaveAll)
        rvSummary = findViewById(R.id.rvSummary)

        updateDateLabel()

        setupRecycler()
        setupListeners()
        
        // Initial load
        loadPaymentsDashboard(currentYear, currentMonth)
    }

    private fun updateDateLabel() {
        val monthName = getMonthName(currentMonth)
        tvDate.text = "$monthName, $currentYear"
    }

    private fun getMonthName(month: Int): String {
        return when(month) {
            1 -> "January"
            2 -> "February"
            3 -> "March"
            4 -> "April"
            5 -> "May"
            6 -> "June"
            7 -> "July"
            8 -> "August"
            9 -> "September"
            10 -> "October"
            11 -> "November"
            12 -> "December"
            else -> ""
        }
    }

    private fun setupRecycler() {
        // We use GridLayoutManager to show cards side-by-side on larger screens if needed, 
        // or just vertical list for now. The screenshot shows side-by-side but on phone it will be vertical.
        // Let's use Span count 1 for mobile, but could be dynamic.
        adapter = CompanyPaymentsAdapter(emptyList())
        rvSummary.layoutManager = GridLayoutManager(this, 1) // Change span to 2 for tablets
        rvSummary.adapter = adapter
    }

    private fun setupListeners() {
        
        tvDate.setOnClickListener {
             showMonthYearPicker()
        }

        btnSaveAll.setOnClickListener {
            saveAllChanges()
        }
    }

    private fun showMonthYearPicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, monthOfYear, _ ->
                currentYear = year
                currentMonth = monthOfYear + 1 // Month is 0-indexed in DatePickerDialog
                updateDateLabel()
                loadPaymentsDashboard(currentYear, currentMonth)
            },
            currentYear,
            currentMonth - 1, // Month is 0-indexed
            1 // Day doesn't matter
        )
        
        datePickerDialog.setTitle("Select Month")
        datePickerDialog.show()
    }

    private fun loadPaymentsDashboard(year: Int, month: Int) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.adminPaymentsApi.getPaymentsDashboard(year, month)
                
                // Update Totals
                tvTotalInvoice.text = "Total Invoice: ₹${response.grand_total_invoice}"
                tvTotalPaid.text = "Total Paid: ₹${response.grand_total_paid}"
                tvTotalDue.text = "Due: ₹${response.grand_total_due}"
                
                allCompanyPayments = response.payments
                
                // Show all by default
                adapter.updateList(allCompanyPayments)

            } catch (e: Exception) {
                Log.e("AdminPayments", "Error loading dashboard", e)
                Toast.makeText(this@AdminDuesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveAllChanges() {
        // Get all modified data
        val dataMap = adapter.getPaymentDataWithContext(currentYear, currentMonth)
        
        if (dataMap.isEmpty()) {
             Toast.makeText(this, "No data to save", Toast.LENGTH_SHORT).show()
             return
        }

        val request = SaveDailyPaymentsRequest(
            year = currentYear,
            month = currentMonth,
            data = dataMap
        )
        
        lifecycleScope.launch {
            try {
                 ApiClient.adminPaymentsApi.saveDailyPayments(request)
                 Toast.makeText(this@AdminDuesActivity, "Saved Successfully!", Toast.LENGTH_SHORT).show()
                 
                 // Reload to refresh totals and clear dirty states if any
                 loadPaymentsDashboard(currentYear, currentMonth)
            } catch (e: Exception) {
                 Log.e("AdminPayments", "Save failed", e)
                 Toast.makeText(this@AdminDuesActivity, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
