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
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.svd.svdagencies.R
import com.svd.svdagencies.base.BaseActivity
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.delivery.DeliveryAgentDuesResponse
import com.svd.svdagencies.data.model.delivery.DeliveryTodayBill
import com.svd.svdagencies.databinding.DeliveryDashboardBinding
import com.svd.svdagencies.utils.SessionManager
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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
        binding.tvSelectedDate.text = apiDate(selectedDate)
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
            onShowQR = { bill -> showQRDialog(bill) }
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
                    val bills = body?.results?.firstOrNull()?.bills ?: emptyList()

                    // Convert to DeliveryTodayBill for the adapter
                    val todayBills = bills.map { 
                        DeliveryTodayBill(
                            id = it.id,
                            billId = it.id,
                            billNumber = it.invoiceNumber,
                            totalAmount = it.totalAmount,
                            date = it.invoiceDate,
                            fileUrl = null
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

        binding.tvAgentBillCount.text = "Generated Bills\n${summary.billCount}"
        binding.tvAgentCounterDue.text = "Invoice Amount\n${money(summary.totalAmount)}"
        binding.tvAgentDeliveredAmount.text = "Paid Amount\n${money(summary.totalPaid)}"
        binding.tvAgentProfit.text = "Current Due\n${money(currentAgentDue(summary))}"
        binding.tvAgentCollectedAmount.text = "Salary Earned\n${money(salaryEarned(summary))}"
        binding.tvAgentRemainingAmount.text = "Salary Paid\n${money(summary.salaryPaid)}"
        binding.tvAgentSelfBillAmount.text = "Pending Salary\n${money(pendingSalary(summary))}"
        binding.tvAgentSubmittedAmount.text = "Customer Bills\n${summary.customerBillCount}"
        binding.tvAgentDueAmount.text = "Self Bills\n${summary.selfBillCount}"
        binding.tvAgentProfitAmount.text = "Items Sale\n${money(summary.relatedInvoiceAmount)}"
        agentItemAdapter.submitList(response.items)
    }

    private fun currentAgentDue(summary: com.svd.svdagencies.data.model.delivery.DeliveryAgentDuesSummary): Double {
        return if (summary.agentCurrentDue != 0.0) summary.agentCurrentDue else summary.totalDue
    }

    private fun salaryEarned(summary: com.svd.svdagencies.data.model.delivery.DeliveryAgentDuesSummary): Double {
        return if (summary.salaryEarned != 0.0) summary.salaryEarned else summary.totalProfit
    }

    private fun pendingSalary(summary: com.svd.svdagencies.data.model.delivery.DeliveryAgentDuesSummary): Double {
        return (salaryEarned(summary) - summary.salaryPaid).coerceAtLeast(0.0)
    }

    private fun submittedAmount(summary: com.svd.svdagencies.data.model.delivery.DeliveryAgentDuesSummary): Double {
        return when {
            summary.submittedAmount > 0.0 -> summary.submittedAmount
            summary.counterSubmitAmount > 0.0 -> summary.counterSubmitAmount
            else -> 0.0
        }
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
            .show()
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

    private fun money(value: Double): String = "Rs. %.2f".format(Locale.US, value)
}
