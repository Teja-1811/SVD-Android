package com.svd.svdagencies.ui.admin

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
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

        updateDateLabel()
        setupRecycler()
        setupListeners()
        loadPaymentsDashboard(currentYear, currentMonth)
    }

    private fun setupRecycler() {
        adapter = CompanyPaymentsAdapter(emptyList())
        rvSummary.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvSummary.adapter = adapter
        
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(rvSummary)

        rvSummary.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val position = layoutManager.findFirstCompletelyVisibleItemPosition()
                if (position != RecyclerView.NO_POSITION) {
                    updateIndicators(position)
                }
            }
        })
    }

    private fun setupIndicators(count: Int) {
        layoutIndicators.removeAllViews()
        if (count <= 1) return

        val indicators = arrayOfNulls<ImageView>(count)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(8, 0, 8, 0)

        for (i in 0 until count) {
            indicators[i] = ImageView(applicationContext)
            indicators[i]?.apply {
                setImageDrawable(ContextCompat.getDrawable(this@AdminDuesActivity, R.drawable.bg_red_dot))
                alpha = 0.3f
                layoutParams = params
            }
            layoutIndicators.addView(indicators[i])
        }
        updateIndicators(0)
    }

    private fun updateIndicators(position: Int) {
        for (i in 0 until layoutIndicators.childCount) {
            val view = layoutIndicators.getChildAt(i) as ImageView
            if (i == position) {
                view.alpha = 1.0f
                view.scaleX = 1.2f
                view.scaleY = 1.2f
            } else {
                view.alpha = 0.3f
                view.scaleX = 1.0f
                view.scaleY = 1.0f
            }
        }
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
            try {
                val response = ApiClient.adminPaymentsApi.getPaymentsDashboard(year, month)
                tvTotalInvoice.text = "₹${response.grand_total_invoice}"
                tvTotalPaid.text = "₹${response.grand_total_paid}"
                tvTotalDue.text = "₹${response.grand_total_due}"
                
                allCompanyPayments = response.payments
                adapter.updateList(allCompanyPayments)
                setupIndicators(allCompanyPayments.size)

            } catch (e: Exception) {
                Toast.makeText(this@AdminDuesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveAllChanges() {
        val dataMap = adapter.getPaymentDataWithContext(currentYear, currentMonth)
        if (dataMap.isEmpty()) return

        lifecycleScope.launch {
            try {
                ApiClient.adminPaymentsApi.saveDailyPayments(SaveDailyPaymentsRequest(currentYear, currentMonth, dataMap))
                Toast.makeText(this@AdminDuesActivity, "Saved Successfully!", Toast.LENGTH_SHORT).show()
                loadPaymentsDashboard(currentYear, currentMonth)
            } catch (e: Exception) {
                Toast.makeText(this@AdminDuesActivity, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
