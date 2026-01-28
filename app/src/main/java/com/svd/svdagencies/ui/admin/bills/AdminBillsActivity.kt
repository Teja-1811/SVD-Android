package com.svd.svdagencies.ui.admin.bills

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.Bills.BillCustomer
import com.svd.svdagencies.ui.admin.adapter.BillAdapter
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AdminBillsActivity : AdminBaseActivity() {

    private lateinit var spinnerCustomer: Spinner
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var btnSearch: Button
    private lateinit var rvBills: RecyclerView
    private lateinit var fabCreateBill: FloatingActionButton

    private lateinit var billAdapter: BillAdapter
    private var customers: List<BillCustomer> = emptyList()

    private var startDate: Calendar = Calendar.getInstance()
    private var endDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_bills_dashboard)
        setupAdminLayout("Bills")

        initViews()
        setupRecyclerView()
        setupListeners()

        fetchCustomers()
        fetchBills()
    }

    private fun initViews() {
        spinnerCustomer = findViewById(R.id.spinnerCustomer)
        tvStartDate = findViewById(R.id.tvStartDate)
        tvEndDate = findViewById(R.id.tvEndDate)
        btnSearch = findViewById(R.id.btnSearch)
        rvBills = findViewById(R.id.rvBills)
        fabCreateBill = findViewById(R.id.fabCreateBill)
    }

    private fun setupRecyclerView() {
        billAdapter = BillAdapter(emptyList())
        rvBills.layoutManager = LinearLayoutManager(this)
        rvBills.adapter = billAdapter
    }

    private fun setupListeners() {
        tvStartDate.setOnClickListener { showDatePickerDialog(isStartDate = true) }
        tvEndDate.setOnClickListener { showDatePickerDialog(isStartDate = false) }
        btnSearch.setOnClickListener { fetchBills() }
        fabCreateBill.setOnClickListener { 
            val intent = Intent(this, CreateBillActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showDatePickerDialog(isStartDate: Boolean) {
        val calendar = if (isStartDate) startDate else endDate
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                updateDateTextView(isStartDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun updateDateTextView(isStartDate: Boolean) {
        val calendar = if (isStartDate) startDate else endDate
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = format.format(calendar.time)
        if (isStartDate) {
            tvStartDate.text = dateString
        } else {
            tvEndDate.text = dateString
        }
    }

    private fun fetchCustomers() {
        lifecycleScope.launch {
            try {
                customers = ApiClient.billsDashboardApi.getCustomersForBill()
                val customerNames = mutableListOf("All Customers")
                customerNames.addAll(customers.map { it.name })
                val adapter = ArrayAdapter(this@AdminBillsActivity, android.R.layout.simple_spinner_item, customerNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerCustomer.adapter = adapter
            } catch (e: Exception) {
                Toast.makeText(this@AdminBillsActivity, "Error fetching customers: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchBills() {
        val customerPosition = spinnerCustomer.selectedItemPosition
        val customerId = if (customerPosition > 0) customers[customerPosition - 1].id else null

        val startDateString = if (tvStartDate.text.isNotEmpty()) tvStartDate.text.toString() else null
        val endDateString = if (tvEndDate.text.isNotEmpty()) tvEndDate.text.toString() else null

        lifecycleScope.launch {
            try {
                val response = ApiClient.billsDashboardApi.getBills(customerId, startDateString, endDateString)
                billAdapter.updateData(response.results)
            } catch (e: Exception) {
                Toast.makeText(this@AdminBillsActivity, "Error fetching bills: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
