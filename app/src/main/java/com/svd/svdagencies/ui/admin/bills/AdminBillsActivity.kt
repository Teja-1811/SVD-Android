package com.svd.svdagencies.ui.admin.bills

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.Bills.AdminBill
import com.svd.svdagencies.data.model.admin.customerData.CustomerItem
import com.svd.svdagencies.ui.admin.adapter.AdminBillAdapter
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import com.svd.svdagencies.utils.RefreshManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

class AdminBillsActivity : AdminBaseActivity() {

    private lateinit var autoCompleteCustomer: AutoCompleteTextView
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var btnSearch: Button
    private lateinit var rvBills: RecyclerView
    private lateinit var fabCreateBill: FloatingActionButton
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var billAdapter: AdminBillAdapter
    private var customers: List<CustomerItem> = emptyList()

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
    }

    override fun onResume() {
        super.onResume()
        // Auto-refresh when returning from Create or Detail screens
        fetchBills()
    }

    private fun initViews() {
        autoCompleteCustomer = findViewById(R.id.autoCompleteCustomer)
        tvStartDate = findViewById(R.id.tvStartDate)
        tvEndDate = findViewById(R.id.tvEndDate)
        btnSearch = findViewById(R.id.btnSearch)
        rvBills = findViewById(R.id.rvBills)
        fabCreateBill = findViewById(R.id.fabCreateBill)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        
        // Use RefreshManager to setup swipe refresh
        RefreshManager.setupRefresh(swipeRefreshLayout) {
            fetchBills()
        }
    }

    private fun setupRecyclerView() {
        billAdapter = AdminBillAdapter(
            emptyList(),
            onViewClick = { bill ->
                val intent = Intent(this, AdminBillDetailActivity::class.java)
                intent.putExtra("bill_id", bill.id)
                startActivity(intent)
            },
            onEditClick = { bill ->
                val intent = Intent(this, CreateBillActivity::class.java).apply {
                    putExtra("bill_id", bill.id)
                }
                startActivity(intent)
            },
            onWhatsappClick = { bill ->
                shareBillOnWhatsappLikeWebsite(bill)
            },
            onDownloadClick = { bill ->
                bill.id.let { downloadBill(it) }
            },
            onDeleteClick = { bill ->
                showDeleteConfirmation(bill.id, bill.bill_number)
            }
        )
        rvBills.layoutManager = LinearLayoutManager(this)
        rvBills.adapter = billAdapter
    }

    private fun shareBillOnWhatsappLikeWebsite(bill: AdminBill) {
        val customer = customers.find { it.name == bill.customer_name }
        var phoneNumber = (bill.customerPhone ?: customer?.phone).orEmpty().trim()
        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "Customer phone number not available", Toast.LENGTH_SHORT).show()
            return
        }

        if (phoneNumber.length == 10) {
            phoneNumber = "91$phoneNumber"
        }

        val customerName = bill.customer_name.orEmpty()
        val agencyName = (bill.customerShopName ?: customer?.shop_name).orEmpty()
        val invoiceNumber = bill.bill_number.orEmpty()
        val invoiceDate = formatShareDate(bill.date)
        val invoiceLink = bill.publicInvoiceUrl.orEmpty()
        val due = bill.currentDue
        val balanceLine = if (due < 0) {
            "Wallet Balance: ₹${kotlin.math.abs(Math.round(due))}"
        } else {
            "Current Due: ₹%.2f".format(Locale.US, due)
        }

        val message = """
            Dear $customerName${if (agencyName.isNotBlank()) " ($agencyName)" else ""},

            Please find your invoice details below:

            Invoice No: $invoiceNumber
            Invoice Date: $invoiceDate
            Invoice Link: $invoiceLink

            $balanceLine

            Thank you for your continued business with
            Sri Vijaya Durga Milk Agencies.
        """.trimIndent()

        try {
            val url = "https://api.whatsapp.com/send/?phone=$phoneNumber&text=" +
                URLEncoder.encode(message, "UTF-8") +
                "&type=phone_number&app_absent=0"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp not installed or error occurred", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatShareDate(dateString: String?): String {
        if (dateString.isNullOrBlank()) return ""
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val outputFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.US)
            val date = inputFormat.parse(dateString)
            if (date != null) outputFormat.format(date) else dateString
        } catch (e: Exception) {
            dateString
        }
    }

    private fun shareBillOnWhatsapp(bill: AdminBill) {
        val customer = customers.find { it.name == bill.customer_name }
        if (customer == null) {
            Toast.makeText(this, "Customer details not found for sharing", Toast.LENGTH_SHORT).show()
            return
        }

        val phoneNumber = customer.phone
        if (phoneNumber.isNullOrEmpty()) {
            Toast.makeText(this, "Customer phone number not available", Toast.LENGTH_SHORT).show()
            return
        }

        val due = customer.due ?: 0.0
        val dueStatusMessage = if (due >= 0) {
            "Current Due: ₹$due"
        } else {
            "Wallet Balance: ₹${Math.abs(due)}"
        }

        val message = """
            *Invoice Details*
            Bill #: ${bill.bill_number}
            Date: ${bill.date}
            Total Amount: ₹${bill.total_amount}
            
            *Customer Details*
            Name: ${bill.customer_name}
            $dueStatusMessage
            
            Visit us: www.svdagencies.shop
            Thank you for your business!
        """.trimIndent()

        try {
            val url = "https://api.whatsapp.com/send?phone=91$phoneNumber&text=" + URLEncoder.encode(message, "UTF-8")
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp not installed or error occurred", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmation(billId: Int?, billNumber: String?) {
        if (billId == null) return
        
        AlertDialog.Builder(this)
            .setTitle("Delete Bill")
            .setMessage("Are you sure you want to delete bill #$billNumber?")
            .setPositiveButton("Delete") { _, _ ->
                deleteBill(billId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteBill(billId: Int) {
        lifecycleScope.launch {
            try {
                ApiClient.billsDashboardApi.deleteBill(billId)
                Toast.makeText(this@AdminBillsActivity, "Bill deleted successfully", Toast.LENGTH_SHORT).show()
                fetchBills() // Refresh the list
            } catch (e: Exception) {
                Toast.makeText(this@AdminBillsActivity, "Error deleting bill: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun downloadBill(billId: Int) {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@AdminBillsActivity, "Downloading bill...", Toast.LENGTH_SHORT).show()
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

                val fileUri = FileProvider.getUriForFile(this@AdminBillsActivity, "${applicationContext.packageName}.provider", file)
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(fileUri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(viewIntent)

            } catch (e: Exception) {
                Toast.makeText(this@AdminBillsActivity, "Error downloading bill: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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
                val response = ApiClient.billsDashboardApi.getCustomersForBill()
                customers = response.customers ?: emptyList()
                val customerNames = customers.map { "${it.name} (${it.shop_name})" }
                val adapter = ArrayAdapter(this@AdminBillsActivity, android.R.layout.simple_dropdown_item_1line, customerNames)
                autoCompleteCustomer.setAdapter(adapter)
            } catch (e: Exception) {
                Toast.makeText(this@AdminBillsActivity, "Error fetching customers: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchBills() {
        RefreshManager.startRefresh(swipeRefreshLayout)
        val selectedText = autoCompleteCustomer.text.toString()
        val customerId = if (selectedText.isNotEmpty()) {
            customers.find { "${it.name} (${it.shop_name})" == selectedText }?.id
        } else null

        val startDateString = if (tvStartDate.text.isNotEmpty()) tvStartDate.text.toString() else null
        val endDateString = if (tvEndDate.text.isNotEmpty()) tvEndDate.text.toString() else null

        lifecycleScope.launch {
            try {
                val response = ApiClient.billsDashboardApi.getBills(customerId, startDateString, endDateString)
                billAdapter.updateList(response.results)
            } catch (e: Exception) {
                Toast.makeText(this@AdminBillsActivity, "Error fetching bills: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                RefreshManager.stopRefresh(swipeRefreshLayout)
            }
        }
    }
}
