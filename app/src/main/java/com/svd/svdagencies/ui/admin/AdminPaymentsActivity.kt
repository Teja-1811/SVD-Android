package com.svd.svdagencies.ui.admin

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.admin.AdminSummaryItem
import com.svd.svdagencies.ui.admin.Adapter.AdminSummaryAdapter
import java.util.Calendar

class AdminPaymentsActivity : AdminBaseActivity() {

    private lateinit var spinnerArea: Spinner
    private lateinit var spinnerCustomer: Spinner
    private lateinit var tvDate: TextView
    private lateinit var btnView: MaterialButton
    private lateinit var btnDownload: MaterialButton
    private lateinit var btnReset: MaterialButton
    
    private lateinit var tvTotalInvoice: TextView
    private lateinit var tvTotalPaid: TextView
    private lateinit var tvTotalDue: TextView
    private lateinit var btnSaveAll: MaterialButton
    private lateinit var rvSummary: RecyclerView
    private lateinit var adapter: AdminSummaryAdapter
    private lateinit var tvCustomerNameHeader: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_monthly_summary)

        setupAdminLayout("Monthly Summary")

        // Views
        spinnerArea = findViewById(R.id.spinnerArea)
        spinnerCustomer = findViewById(R.id.spinnerCustomer)
        tvDate = findViewById(R.id.tvDate)
        
        btnView = findViewById(R.id.btnView)
        btnDownload = findViewById(R.id.btnDownload)
        btnReset = findViewById(R.id.btnReset)

        tvTotalInvoice = findViewById(R.id.tvTotalInvoice)
        tvTotalPaid = findViewById(R.id.tvTotalPaid)
        tvTotalDue = findViewById(R.id.tvTotalDue)
        btnSaveAll = findViewById(R.id.btnSaveAll)
        rvSummary = findViewById(R.id.rvSummary)
        tvCustomerNameHeader = findViewById(R.id.tvCustomerNameHeader)

        setupSpinners()
        setupRecycler()
        setupListeners()
        
        // Initial load (mock)
        loadSummaryData()
    }

    private fun setupSpinners() {
        // Mock Areas
        val areas = listOf("-- All Areas --", "Area 1", "Area 2")
        val areaAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, areas)
        areaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerArea.adapter = areaAdapter
        
        // Mock Customers
        val customers = listOf("A Sivayya", "Customer B", "Customer C")
        val customerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, customers)
        customerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCustomer.adapter = customerAdapter
    }

    private fun setupRecycler() {
        adapter = AdminSummaryAdapter(emptyList())
        rvSummary.layoutManager = LinearLayoutManager(this)
        rvSummary.adapter = adapter
    }

    private fun setupListeners() {
        
        tvDate.setOnClickListener {
             // Date Picker logic would go here
             Toast.makeText(this, "Select Month clicked", Toast.LENGTH_SHORT).show()
        }

        btnView.setOnClickListener {
            loadSummaryData()
            Toast.makeText(this, "View clicked", Toast.LENGTH_SHORT).show()
        }
        
        btnDownload.setOnClickListener {
            Toast.makeText(this, "Download PDF clicked", Toast.LENGTH_SHORT).show()
        }
        
        btnReset.setOnClickListener {
            Toast.makeText(this, "Reset clicked", Toast.LENGTH_SHORT).show()
        }

        btnSaveAll.setOnClickListener {
            Toast.makeText(this, "Saved Successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSummaryData() {
        // Mock data to match screenshot
        // Totals
        tvTotalInvoice.text = "Total Invoice: ₹47608.01"
        tvTotalPaid.text = "Total Paid: ₹29113.23"
        tvTotalDue.text = "Due: ₹18494.78"
        
        tvCustomerNameHeader.text = "Dodla"

        // List Items
        val mockData = listOf(
            AdminSummaryItem("01", 16201.30, 16569.23),
            AdminSummaryItem("02", 10944.24, 10422.00),
            AdminSummaryItem("03", 17528.47, 0.0),
            AdminSummaryItem("04", 0.0, 0.0),
            AdminSummaryItem("05", 0.0, 0.0),
            AdminSummaryItem("06", 0.0, 0.0),
            AdminSummaryItem("07", 0.0, 0.0),
            AdminSummaryItem("08", 0.0, 0.0),
            AdminSummaryItem("09", 0.0, 0.0),
            AdminSummaryItem("10", 0.0, 0.0)
        )
        
        adapter.updateList(mockData)
    }
}
