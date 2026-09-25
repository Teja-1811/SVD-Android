package com.svd.svdagencies.ui.delivery

import com.svd.svdagencies.utils.PaymentConfig

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.svd.svdagencies.R
import com.svd.svdagencies.base.BaseActivity
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.delivery.DeliveryTodayBill
import com.svd.svdagencies.utils.SessionManager
import kotlinx.coroutines.launch
import retrofit2.awaitResponse
import java.net.URLEncoder
import java.util.Locale

class DeliveryBillHistoryActivity : BaseActivity() {

    private var customerId: Int = 0
    private var historyCustomerName: String = ""
    private var historyCustomerPhone: String = ""
    private lateinit var rvTodayBills: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var layoutEmpty: View
    private lateinit var cardSummary: View
    private lateinit var tvTotalBillsCount: TextView
    private lateinit var tvTotalInvoiceAmount: TextView
    private lateinit var todayBillAdapter: DeliveryTodayBillAdapter
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.delivery_bill_history)

        sessionManager = SessionManager(this)
        customerId = intent.getIntExtra("customer_id", 0)
        historyCustomerName = intent.getStringExtra("customer_name").orEmpty()
        historyCustomerPhone = intent.getStringExtra("customer_phone").orEmpty()

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = "Today's Bills"
        toolbar.setNavigationOnClickListener { finish() }

        rvTodayBills = findViewById(R.id.rvTodayBills)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        cardSummary = findViewById(R.id.cardSummary)
        tvTotalBillsCount = findViewById(R.id.tvTotalBillsCount)
        tvTotalInvoiceAmount = findViewById(R.id.tvTotalInvoiceAmount)

        setupRecyclerView()
        setupListeners()
        fetchBills()
    }

    private fun setupRecyclerView() {
        todayBillAdapter = DeliveryTodayBillAdapter(
            onViewBill = { bill -> showBillDetails(bill) },
            onDeleteBill = { bill -> confirmDeleteBill(bill) },
            onShowQR = { bill -> showQRDialog(bill) },
            onShareWhatsapp = { bill -> shareBillOnWhatsapp(bill) }
        )
        rvTodayBills.layoutManager = LinearLayoutManager(this)
        rvTodayBills.adapter = todayBillAdapter
    }

    private fun setupListeners() {
        swipeRefresh.setOnRefreshListener { fetchBills() }
    }

    private fun fetchBills() {
        val targetId = if (customerId > 0) customerId else sessionManager.getUserId()
        if (targetId <= 0) return

        lifecycleScope.launch {
            swipeRefresh.isRefreshing = true
            try {
                val response = ApiClient.deliveryApi.getTodayBills(targetId).awaitResponse()
                if (response.isSuccessful) {
                    val body = response.body()
                    val bills = (body?.bills ?: emptyList()).map { bill ->
                        bill.copy(
                            customerName = bill.customerName?.takeIf { it.isNotBlank() } ?: bill.customer?.takeIf { it.isNotBlank() } ?: historyCustomerName,
                            customerPhone = bill.customerPhone?.takeIf { it.isNotBlank() } ?: historyCustomerPhone
                        )
                    }
                    val totalAmount = body?.totalInvoiceAmount ?: 0.0

                    todayBillAdapter.submitList(bills)
                    layoutEmpty.visibility = if (bills.isEmpty()) View.VISIBLE else View.GONE
                    
                    if (bills.isNotEmpty()) {
                        cardSummary.visibility = View.VISIBLE
                        tvTotalBillsCount.text = "${bills.size} Bills"
                        tvTotalInvoiceAmount.text = money(totalAmount)
                    } else {
                        cardSummary.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(
                        this@DeliveryBillHistoryActivity,
                        com.svd.svdagencies.utils.NetworkMessageUtils.parseError(response, "Failed to load bills"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DeliveryBillHistoryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showBillDetails(bill: DeliveryTodayBill) {
        val dialogView = layoutInflater.inflate(R.layout.delivery_bill_details, null)
        val tvBillNumber = dialogView.findViewById<TextView>(R.id.tvDialogBillNumber)
        val tvTotalAmount = dialogView.findViewById<TextView>(R.id.tvDialogTotalAmount)
        val rvItems = dialogView.findViewById<RecyclerView>(R.id.rvBillDetailItems)
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
                val response = ApiClient.deliveryApi.getBillDetails(bill.realId).awaitResponse()
                if (response.isSuccessful) {
                    val detail = response.body()
                    detail?.let { res ->
                        tvTotalAmount.text = money(res.total_amount)
                        res.items?.let { items ->
                            detailAdapter.submitList(items)
                        } ?: run {
                            try {
                                val itemResponse = ApiClient.billsDashboardApi.getBillItems(bill.realId)
                                detailAdapter.submitList(itemResponse)
                            } catch (e: Exception) {
                                Toast.makeText(this@DeliveryBillHistoryActivity, "Unable to load item breakdown", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@DeliveryBillHistoryActivity, "Error loading details", Toast.LENGTH_SHORT).show()
            }
        }
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
                Toast.makeText(this@DeliveryBillHistoryActivity, "Bill deleted", Toast.LENGTH_SHORT).show()
                fetchBills()
            } catch (e: Exception) {
                Toast.makeText(this@DeliveryBillHistoryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun shareBillOnWhatsapp(bill: DeliveryTodayBill) {
        var phoneNumber = (bill.customerPhone?.takeIf { it.isNotBlank() } ?: historyCustomerPhone).filter { it.isDigit() }
        if (phoneNumber.isEmpty()) {
            fetchCustomerPhoneThenShare(bill)
            return
        }

        if (phoneNumber.length == 10) {
            phoneNumber = "91$phoneNumber"
        }
        if (phoneNumber.length > 10 && !phoneNumber.startsWith("91")) {
            phoneNumber = "91${phoneNumber.takeLast(10)}"
        }

        val customerName = bill.customerName?.takeIf { it.isNotBlank() } ?: bill.customer?.takeIf { it.isNotBlank() } ?: historyCustomerName
        val agencyName = bill.customerShopName.orEmpty()
        val billNo = bill.billNumber ?: "#${bill.realId}"
        val dateStr = formatShareDate(bill.date)
        val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
        val invoiceLink = if (!bill.publicInvoiceUrl.isNullOrBlank()) {
            if (bill.publicInvoiceUrl.startsWith("http")) {
                bill.publicInvoiceUrl
            } else {
                "$baseUrl${if (bill.publicInvoiceUrl.startsWith("/")) "" else "/"}${bill.publicInvoiceUrl}"
            }
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
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp not installed or error occurred", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchCustomerPhoneThenShare(bill: DeliveryTodayBill) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.deliveryApi.getBillCustomers().awaitResponse()
                val customer = response.body()?.results?.firstOrNull { it.id == customerId }
                val phone = customer?.phone.orEmpty()
                if (phone.isBlank()) {
                    Toast.makeText(this@DeliveryBillHistoryActivity, "Customer phone number not available", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                historyCustomerPhone = phone
                if (historyCustomerName.isBlank()) {
                    historyCustomerName = customer?.name.orEmpty()
                }
                shareBillOnWhatsapp(
                    bill.copy(
                        customerName = bill.customerName?.takeIf { it.isNotBlank() } ?: historyCustomerName,
                        customerPhone = phone,
                        customerShopName = bill.customerShopName?.takeIf { it.isNotBlank() } ?: customer?.shopName.orEmpty()
                    )
                )
            } catch (e: Exception) {
                Toast.makeText(this@DeliveryBillHistoryActivity, "Customer phone number not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatShareDate(dateString: String?): String {
        if (dateString.isNullOrBlank()) return ""
        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val outputFormat = java.text.SimpleDateFormat("dd-MMM-yyyy", Locale.US)
            val date = inputFormat.parse(dateString)
            if (date != null) outputFormat.format(date) else dateString
        } catch (e: Exception) {
            dateString
        }
    }

    private fun money(value: Double): String = "₹ %.2f".format(Locale.US, value)
}
