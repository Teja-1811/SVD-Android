package com.svd.svdagencies.ui.admin.bills

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.ui.admin.adapter.BillItemAdapter
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AdminBillDetailActivity : AdminBaseActivity() {

    private var billId: Int = -1

    // Views
    private lateinit var tvCustomerName: TextView
    private lateinit var tvBillNumber: TextView
    private lateinit var tvBillDate: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvProfit: TextView
    private lateinit var tvOpeningDue: TextView
    private lateinit var tvCurrentDue: TextView
    private lateinit var rvBillItems: RecyclerView
    private lateinit var btnEdit: MaterialButton
    private lateinit var btnDelete: MaterialButton
    private lateinit var btnDownload: MaterialButton
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    
    private lateinit var tvBillItemsCount: TextView
    private lateinit var tvFooterTotalAmount: TextView

    private lateinit var billItemAdapter: BillItemAdapter

    private var requestsCompleted = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_bill_info)
        setupAdminLayout("Bill Detail")

        billId = intent.getIntExtra("bill_id", -1)
        if (billId == -1) {
            Toast.makeText(this, "Error: Bill ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupRecyclerView()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    private fun initViews() {
        tvCustomerName = findViewById(R.id.tvCustomerName)
        tvBillNumber = findViewById(R.id.tvBillNumber)
        tvBillDate = findViewById(R.id.tvBillDate)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        tvProfit = findViewById(R.id.tvProfit)
        tvOpeningDue = findViewById(R.id.tvOpeningDue)
        tvCurrentDue = findViewById(R.id.tvCurrentDue)
        rvBillItems = findViewById(R.id.rvBillItems)
        btnEdit = findViewById(R.id.btnEdit)
        btnDelete = findViewById(R.id.btnDelete)
        btnDownload = findViewById(R.id.btnDownload)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        
        tvBillItemsCount = findViewById(R.id.tvBillItemsCount)
        tvFooterTotalAmount = findViewById(R.id.tvFooterTotalAmount)
    }

    private fun setupRecyclerView() {
        billItemAdapter = BillItemAdapter(emptyList())
        rvBillItems.layoutManager = LinearLayoutManager(this)
        rvBillItems.adapter = billItemAdapter
    }

    private fun setupListeners() {
        btnEdit.setOnClickListener { 
            val intent = Intent(this, CreateBillActivity::class.java).apply {
                putExtra("bill_id", billId)
            }
            startActivity(intent)
        }

        btnDelete.setOnClickListener { 
            AlertDialog.Builder(this)
                .setTitle("Delete Bill")
                .setMessage("Are you sure you want to delete this bill? This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ -> deleteBill() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnDownload.setOnClickListener { downloadBill() }

        swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }
    }

    private fun refreshData() {
        requestsCompleted = 0
        swipeRefreshLayout.isRefreshing = true
        fetchBillDetails()
        fetchBillItems()
    }

    private fun fetchBillDetails() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.billsDashboardApi.getBillDetail(billId)
                tvCustomerName.text = response.customer
                tvBillNumber.text = "#${response.invoice_number}"
                tvBillDate.text = response.invoice_date
                tvTotalAmount.text = "₹${response.total_amount}"
                tvProfit.text = "₹${response.profit}"
                tvOpeningDue.text = "₹${response.op_due_amount}"
                tvCurrentDue.text = "₹${response.current_due}"
            } catch (e: Exception) {
                if (!isFinishing) {
                    Toast.makeText(this@AdminBillDetailActivity, "Error fetching bill details: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                checkRefreshStatus()
            }
        }
    }

    private fun fetchBillItems() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.billsDashboardApi.getBillItems(billId)
                billItemAdapter.updateData(response)
                
                // Update table UI
                tvBillItemsCount.text = "Bill Items (${response.size} items)"
                val total = response.sumOf { it.total_amount }
                tvFooterTotalAmount.text = "₹%.2f".format(total)

            } catch (e: Exception) {
                if (!isFinishing) {
                    Toast.makeText(this@AdminBillDetailActivity, "Error fetching bill items: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                checkRefreshStatus()
            }
        }
    }

    private fun checkRefreshStatus() {
        synchronized(this) {
            requestsCompleted++
            if (requestsCompleted >= 2) {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun deleteBill() {
        lifecycleScope.launch {
            try {
                ApiClient.billsDashboardApi.deleteBill(billId)
                Toast.makeText(this@AdminBillDetailActivity, "Bill deleted successfully", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AdminBillDetailActivity, "Error deleting bill: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun downloadBill() {
        lifecycleScope.launch {
            try {
                val responseBody = ApiClient.billsDashboardApi.downloadBill(billId)
                val fileName = "bill_$billId.pdf"
                val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                
                withContext(Dispatchers.IO) {
                    val inputStream = responseBody.byteStream()
                    val outputStream = FileOutputStream(file)
                    inputStream.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                val fileUri = FileProvider.getUriForFile(this@AdminBillDetailActivity, "${applicationContext.packageName}.provider", file)
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(fileUri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(viewIntent)

            } catch (e: Exception) {
                Toast.makeText(this@AdminBillDetailActivity, "Error downloading bill: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}