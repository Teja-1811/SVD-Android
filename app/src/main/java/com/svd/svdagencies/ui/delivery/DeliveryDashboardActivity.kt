package com.svd.svdagencies.ui.delivery

import com.svd.svdagencies.utils.PaymentConfig

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.svd.svdagencies.utils.showDestructiveDialog
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.svd.svdagencies.R
import com.svd.svdagencies.base.BaseActivity
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.delivery.DeliveryAgentDuesResponse
import com.svd.svdagencies.data.model.delivery.DeliveryAgentDuesSummary
import com.svd.svdagencies.data.model.delivery.DeliveryTodayBill
import com.svd.svdagencies.data.model.delivery.DeliveryTodayBillItem
import com.svd.svdagencies.databinding.DeliveryDashboardBinding
import com.svd.svdagencies.utils.SessionManager
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.URLEncoder
import retrofit2.awaitResponse
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DeliveryDashboardActivity : BaseActivity() {

    private lateinit var binding: DeliveryDashboardBinding
    private lateinit var todayBillAdapter: DeliveryTodayBillAdapter
    private lateinit var agentItemAdapter: DeliveryAgentItemAdapter
    private lateinit var sessionManager: SessionManager
    private var selectedDate = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DeliveryDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sessionManager = SessionManager(this)

        setupToolbar(binding.toolbar, "Report")
        DeliveryNavigation.setup(
            this,
            binding.deliveryDrawerLayout,
            binding.deliveryNavigationView,
            menuButton = binding.toolbar.findViewById(R.id.btnMenu),
            selectedItemId = R.id.nav_delivery_today_report
        )
        setupRecyclerViews()
        setupListeners()
        binding.layoutEmpty.findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lottieEmpty)?.setFailureListener { e ->
            android.util.Log.e("Lottie", "Failed to load empty state animation", e)
        }
        setupDatePicker()
        refreshData()
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    private fun setupDatePicker() {
        updateDateDisplay()
        binding.layoutDatePicker.setOnClickListener {
            val datePickerDialog = android.app.DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedDate.set(Calendar.YEAR, year)
                    selectedDate.set(Calendar.MONTH, month)
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    updateDateDisplay()
                    refreshData()
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }
    }

    private fun updateDateDisplay() {
        binding.tvSelectedDate.text = SimpleDateFormat("dd MMM yyyy", Locale.US)
            .format(selectedDate.time)
    }

    private fun setupToolbar(toolbar: Toolbar, title: String) {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.findViewById<TextView>(R.id.tvToolbarTitle)?.text = title
    }

    private fun setupRecyclerViews() {
        // Today's Bills Adapter
        todayBillAdapter = DeliveryTodayBillAdapter(
            onViewBill = { bill -> showBillDetails(bill) },
            onDeleteBill = { bill -> confirmDeleteBill(bill) },
            onShowQR = { bill -> showQRDialog(bill) },
            onShareWhatsapp = { bill -> shareBillOnWhatsapp(bill) }
        )
        binding.rvTodayBills.apply {
            layoutManager = LinearLayoutManager(this@DeliveryDashboardActivity)
            adapter = todayBillAdapter
            isNestedScrollingEnabled = false
        }

        // Agent Report Adapters (Monthly)
        agentItemAdapter = DeliveryAgentItemAdapter()
        binding.rvAgentItems.apply {
            layoutManager = LinearLayoutManager(this@DeliveryDashboardActivity)
            adapter = agentItemAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            refreshData()
        }
        binding.btnViewOverallReport.setOnClickListener {
            startActivity(Intent(this, DeliveryOverallReportActivity::class.java))
        }
    }

    private fun refreshData() {
        loadTodayBills()
    }

    private fun loadTodayBills() {
        binding.swipeRefresh.isRefreshing = true
        val dateStr = apiDate(selectedDate)
        
        ApiClient.deliveryApi.getAgentSummary(dateStr).enqueue(object : Callback<DeliveryAgentDuesResponse> {
            override fun onResponse(call: Call<DeliveryAgentDuesResponse>, response: Response<DeliveryAgentDuesResponse>) {
                binding.swipeRefresh.isRefreshing = false
                if (response.isSuccessful) {
                    val body = response.body()
                    val bills = when {
                        body?.billRows?.isNotEmpty() == true -> body.billRows
                        body?.results?.isNotEmpty() == true -> body.results.flatMap { it.bills }
                        else -> emptyList()
                    }

                    // Convert to DeliveryTodayBill for the adapter
                    val todayBills = bills.map { 
                        DeliveryTodayBill(
                            id = it.id,
                            billId = it.id,
                            billNumber = it.invoiceNumber,
                            invoiceAmount = it.totalAmount,
                            date = it.invoiceDate,
                            publicInvoiceUrl = it.publicInvoiceUrl ?: it.fileUrl,
                            customerName = it.customer,
                            customerPhone = it.customerPhone,
                            customerShopName = it.customerShopName,
                            openingDue = it.openingDue,
                            paid = it.paidAmount,
                            remainingDue = it.remainingDue,
                            dueAmount = it.dueAmount,
                            items = it.items.map { line ->
                                DeliveryTodayBillItem(
                                    name = line.itemName,
                                    quantity = line.quantity,
                                    pricePerUnit = line.pricePerUnit,
                                    discount = line.totalDiscount,
                                    totalAmount = line.amount
                                )
                            }
                        )
                    }

                    todayBillAdapter.submitList(todayBills)
                    
                    binding.rvTodayBills.visibility = if (todayBills.isEmpty()) View.GONE else View.VISIBLE
                    binding.layoutEmpty.visibility = if (todayBills.isEmpty()) View.VISIBLE else View.GONE
                    
                    // Also update agent statistics for this specific date
                    body?.let { renderAgentSummary(it) }
                } else {
                    Toast.makeText(
                        this@DeliveryDashboardActivity,
                        com.svd.svdagencies.utils.NetworkMessageUtils.parseError(response, "Error loading report"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<DeliveryAgentDuesResponse>, t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(this@DeliveryDashboardActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun renderAgentSummary(response: DeliveryAgentDuesResponse) {
        val summary = response.summary
        val dateStr = apiDate(selectedDate)
        val todayStr = apiDate(Calendar.getInstance())
        
        binding.tvAgentDuesTitle.text = if (dateStr == todayStr) {
            "${response.agent?.name ?: "Your"} Today Dues"
        } else {
            "${response.agent?.name ?: "Your"} Dues for $dateStr"
        }

        val counterDueVal = counterDue(summary)
        val profitVal = summary.totalProfit
        val salaryEarnedVal = salaryEarned(summary)
        val salaryPaidVal = summary.salaryPaid
        val pendingSalaryVal = pendingSalary(summary)
        val collectedVal = if (summary.collectedAmount > 0.0) summary.collectedAmount else summary.amountToSubmit

        binding.tvAgentBillCount.text = "Generated Bills\n${summary.billCount}"
        binding.tvAgentCounterDue.text = "Bill Amount\n${money(summary.totalAmount)}"
        binding.tvAgentDeliveredAmount.text = "Paid\n${money(summary.totalPaid)}"
        binding.tvAgentProfit.text = "Bill Due\n${money(counterDueVal)}"
        binding.tvAgentCollectedAmount.text = "Total Profit\n${money(profitVal)}"
        binding.tvAgentHoldingAmount.text = "Collected\n${money(collectedVal)}"
        binding.tvAgentSalaryEarned.text = "Salary Earned\n${money(salaryEarnedVal)}"
        binding.tvAgentRemainingAmount.text = "Salary Paid\n${money(salaryPaidVal)}"
        binding.tvAgentSelfBillAmount.text = "Pending Salary\n${money(pendingSalaryVal)}"
        binding.tvAgentSubmittedAmount.text = "Submitted\n${money(summary.submittedAmount)}"
        binding.tvAgentDueAmount.text = "Remaining to Submit\n${money(summary.remainingGeneratedAmount)}"
        binding.tvAgentProfitAmount.text = "Customer / Self Sales\n${money(summary.customerBillAmount)} / ${money(summary.selfBillAmount)}"
        agentItemAdapter.submitList(response.items)
    }

    private fun counterDue(summary: DeliveryAgentDuesSummary): Double {
        return when {
            summary.counterDue > 0.0 -> summary.counterDue
            summary.totalCounterDue > 0.0 -> summary.totalCounterDue
            summary.totalDueAmount > 0.0 -> summary.totalDueAmount
            summary.totalDue > 0.0 -> summary.totalDue
            else -> (summary.totalAmount - summary.totalPaid).coerceAtLeast(0.0)
        }
    }

    private fun salaryEarned(summary: DeliveryAgentDuesSummary): Double {
        return if (summary.salaryEarned != 0.0) summary.salaryEarned else summary.totalProfit
    }

    private fun pendingSalary(summary: DeliveryAgentDuesSummary): Double {
        return (salaryEarned(summary) - summary.salaryPaid).coerceAtLeast(0.0)
    }

    private fun showBillDetails(bill: DeliveryTodayBill) {
        val dialogView = layoutInflater.inflate(R.layout.delivery_bill_details, null)
        val tvBillNumber = dialogView.findViewById<TextView>(R.id.tvDialogBillNumber)
        val tvTotalAmount = dialogView.findViewById<TextView>(R.id.tvDialogTotalAmount)
        val rvItems = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvBillDetailItems)
        val btnClose = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCloseDialog)

        tvBillNumber.text = bill.billNumber ?: "#${bill.realId}"
        tvTotalAmount.text = money(bill.totalAmount)

        val detailAdapter = BillDetailItemAdapter()
        rvItems.layoutManager = LinearLayoutManager(this)
        rvItems.adapter = detailAdapter

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()

        lifecycleScope.launch {
            try {
                // First, fetch the bill summary details
                val response = ApiClient.deliveryApi.getBillDetails(bill.realId).awaitResponse()
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let { res ->
                        tvTotalAmount.text = money(res.total_amount)
                    }
                }

                // Second, fetch the items list from the separate endpoint identified in logs
                val itemsResponse = ApiClient.deliveryApi.getBillItemsDetail(bill.realId).awaitResponse()
                if (itemsResponse.isSuccessful) {
                    val items = itemsResponse.body()
                    items?.let { itemsList ->
                        detailAdapter.submitList(itemsList)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@DeliveryDashboardActivity, "Error loading details", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showQRDialog(bill: DeliveryTodayBill) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_qr_code, null)
        val ivQrCode = dialogView.findViewById<ImageView>(R.id.ivQrCode)
        val tvQrAmount = dialogView.findViewById<TextView>(R.id.tvQrAmount)
        val tvBillInfo = dialogView.findViewById<TextView>(R.id.tvBillInfo)
        val btnCloseQr = dialogView.findViewById<MaterialButton>(R.id.btnCloseQr)

        val amount = bill.totalAmount
        val billNo = bill.billNumber ?: "#${bill.realId}"

        tvQrAmount.text = "Amount: ${money(amount)}"
        tvBillInfo.text = "Invoice: $billNo"
        tvBillInfo.visibility = View.VISIBLE

        val upiId = PaymentConfig.UPI_ID
        val name = "Sri Vijay Durga Milk Agency"
        val upiUrl = "upi://pay?pa=$upiId&pn=${Uri.encode(name)}&am=${"%.2f".format(amount)}&cu=INR&tn=${Uri.encode(billNo)}"

        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(upiUrl, BarcodeFormat.QR_CODE, 512, 512)
            ivQrCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error generating QR code", Toast.LENGTH_SHORT).show()
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        btnCloseQr.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun confirmDeleteBill(bill: DeliveryTodayBill) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Bill")
            .setMessage("Are you sure you want to delete bill ${bill.billNumber ?: "#${bill.realId}"}?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                deleteBill(bill)
            }
            .showDestructiveDialog()
    }

    private fun shareBillOnWhatsapp(bill: DeliveryTodayBill) {
        var phoneNumber = bill.customerPhone.orEmpty().filter { it.isDigit() }
        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "Customer phone number not available", Toast.LENGTH_SHORT).show()
            return
        }

        if (phoneNumber.length == 10) {
            phoneNumber = "91$phoneNumber"
        }
        if (phoneNumber.length > 10 && !phoneNumber.startsWith("91")) {
            phoneNumber = "91${phoneNumber.takeLast(10)}"
        }

        val customerName = bill.customerName?.takeIf { it.isNotBlank() } ?: bill.customer.orEmpty()
        val agencyName = bill.customerShopName.orEmpty()
        val billNo = bill.billNumber ?: "#${bill.realId}"
        val dateStr = formatShareDate(bill.date)
        val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
        val invoiceLink = if (!bill.publicInvoiceUrl.isNullOrBlank()) {
            if (bill.publicInvoiceUrl.startsWith("http")) bill.publicInvoiceUrl else "$baseUrl${if (bill.publicInvoiceUrl.startsWith("/")) "" else "/"}${bill.publicInvoiceUrl}"
        } else {
            "$baseUrl/api/bills/${bill.realId}/download/"
        }

        val due = if (bill.currentDue != 0.0) bill.currentDue else bill.dueAmount
        val balanceLine = if (due < 0) {
            "Wallet Balance: ₹${kotlin.math.abs(Math.round(due))}"
        } else {
            "Current Due: ₹%.2f".format(Locale.US, due)
        }

        val message = """
            Dear $customerName${if (agencyName.isNotBlank()) " ($agencyName)" else ""},

            Please find your invoice details below:

            Invoice No: $billNo
            Invoice Date: $dateStr
            Invoice Link: $invoiceLink

            $balanceLine

            Thank you for your continued business with
            Sri Vijaya Durga Milk Agencies.
        """.trimIndent()

        try {
            val url = "https://api.whatsapp.com/send/?phone=$phoneNumber&text=" +
                URLEncoder.encode(message, "UTF-8") +
                "&type=phone_number&app_absent=0"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp not installed or error occurred", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteBill(bill: DeliveryTodayBill) {
        lifecycleScope.launch {
            try {
                ApiClient.billsDashboardApi.deleteBill(bill.realId)
                Toast.makeText(this@DeliveryDashboardActivity, "Bill deleted", Toast.LENGTH_SHORT).show()
                refreshData()
            } catch (e: Exception) {
                Toast.makeText(this@DeliveryDashboardActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun apiDate(calendar: Calendar): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
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

    private fun money(value: Double): String = "Rs. %.2f".format(Locale.US, value)
}
