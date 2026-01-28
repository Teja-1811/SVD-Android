package com.svd.svdagencies.ui.admin.bills

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var tvInvoiceNumber: TextView
    private lateinit var tvInvoiceDate: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvProfit: TextView
    private lateinit var tvOpeningDue: TextView
    private lateinit var tvCurrentDue: TextView
    private lateinit var rvBillItems: RecyclerView
    private lateinit var btnEditBill: Button
    private lateinit var btnDeleteBill: Button
    private lateinit var btnDownloadBill: Button

    private lateinit var billItemAdapter: BillItemAdapter

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

        fetchBillDetails()
        fetchBillItems()
    }

    private fun initViews() {
        tvCustomerName = findViewById(R.id.tvCustomerName)
        tvInvoiceNumber = findViewById(R.id.tvInvoiceNumber)
        tvInvoiceDate = findViewById(R.id.tvInvoiceDate)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        tvProfit = findViewById(R.id.tvProfit)
        tvOpeningDue = findViewById(R.id.tvOpeningDue)
        tvCurrentDue = findViewById(R.id.tvCurrentDue)
        rvBillItems = findViewById(R.id.rvBillItems)
        btnEditBill = findViewById(R.id.btnEditBill)
        btnDeleteBill = findViewById(R.id.btnDeleteBill)
        btnDownloadBill = findViewById(R.id.btnDownloadBill)
    }

    private fun setupRecyclerView() {
        billItemAdapter = BillItemAdapter(emptyList())
        rvBillItems.layoutManager = LinearLayoutManager(this)
        rvBillItems.adapter = billItemAdapter
    }

    private fun setupListeners() {
        btnEditBill.setOnClickListener { 
            val intent = Intent(this, CreateBillActivity::class.java).apply {
                putExtra("bill_id", billId)
            }
            startActivity(intent)
        }

        btnDeleteBill.setOnClickListener { 
            AlertDialog.Builder(this)
                .setTitle("Delete Bill")
                .setMessage("Are you sure you want to delete this bill? This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ -> deleteBill() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnDownloadBill.setOnClickListener { downloadBill() }
    }

    private fun fetchBillDetails() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.billsDashboardApi.getBillDetail(billId)
                tvCustomerName.text = response.customer
                tvInvoiceNumber.text = "Invoice: ${response.invoice_number}"
                tvInvoiceDate.text = "Date: ${response.invoice_date}"
                tvTotalAmount.text = "Total\n₹${response.total_amount}"
                tvProfit.text = "Profit\n₹${response.profit}"
                tvOpeningDue.text = "Opening Due\n₹${response.op_due_amount}"
                tvCurrentDue.text = "Current Due\n₹${response.current_due}"
            } catch (e: Exception) {
                Toast.makeText(this@AdminBillDetailActivity, "Error fetching bill details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchBillItems() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.billsDashboardApi.getBillItems(billId)
                billItemAdapter.updateData(response)
            } catch (e: Exception) {
                Toast.makeText(this@AdminBillDetailActivity, "Error fetching bill items: ${e.message}", Toast.LENGTH_SHORT).show()
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
