package com.svd.svdagencies.ui.customer.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.api.customer.CustomerApi
import com.svd.svdagencies.data.model.customer.CustomerDashboardResponse
import com.svd.svdagencies.data.model.customer.GenericResponse
import com.svd.svdagencies.data.model.customer.PaymentGatewayInitResponse
import com.svd.svdagencies.ui.customer.adapter.CustomerPaymentHistoryAdapter
import com.svd.svdagencies.utils.SessionManager
import com.svd.svdagencies.utils.showLoading
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.awaitResponse
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CustomerPaymentFragment : Fragment() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var etAmount: EditText
    private lateinit var btnUpi: MaterialButton
    private lateinit var tvCurrentDue: TextView
    private lateinit var tvCurrentBalance: TextView

    private lateinit var rvHistory: RecyclerView
    private lateinit var tvMonthLabel: TextView
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private lateinit var layoutEmptyHistory: View

    private val historyAdapter = CustomerPaymentHistoryAdapter()
    private val selectedMonth = Calendar.getInstance()
    private val monthLabelFormat = SimpleDateFormat("MMM yyyy", Locale.US)

    private lateinit var api: CustomerApi
    private lateinit var session: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.customer_payment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        etAmount = view.findViewById(R.id.etPayAmount)
        btnUpi = view.findViewById(R.id.btnPayUpi)
        tvCurrentDue = view.findViewById(R.id.tvCurrentDue)
        tvCurrentBalance = view.findViewById(R.id.tvCurrentBalance)

        rvHistory = view.findViewById(R.id.rvPaymentHistory)
        tvMonthLabel = view.findViewById(R.id.tvMonthLabel)
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth)
        btnNextMonth = view.findViewById(R.id.btnNextMonth)
        layoutEmptyHistory = view.findViewById(R.id.layoutEmptyHistory)

        session = SessionManager(requireContext())
        if (session.getUserId() == -1) {
            showToast(getString(R.string.session_expired))
            return
        }

        api = ApiClient.customerApi

        btnUpi.setOnClickListener { startGatewayPayment() }
        swipeRefresh.setOnRefreshListener { loadAllData() }

        setupHistoryList()
        setupMonthFilter()
        loadAllData()
    }

    private fun setupHistoryList() {
        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        rvHistory.adapter = historyAdapter
    }

    private fun setupMonthFilter() {
        updateMonthLabel()
        btnPrevMonth.setOnClickListener {
            selectedMonth.add(Calendar.MONTH, -1)
            updateMonthLabel()
            loadPaymentHistory()
        }
        btnNextMonth.setOnClickListener {
            selectedMonth.add(Calendar.MONTH, 1)
            updateMonthLabel()
            loadPaymentHistory()
        }
        tvMonthLabel.setOnClickListener { showMonthPicker() }
    }

    private fun showMonthPicker() {
        android.app.DatePickerDialog(
            requireContext(),
            { _, year, month, _ ->
                selectedMonth.set(Calendar.YEAR, year)
                selectedMonth.set(Calendar.MONTH, month)
                selectedMonth.set(Calendar.DAY_OF_MONTH, 1)
                updateMonthLabel()
                loadPaymentHistory()
            },
            selectedMonth.get(Calendar.YEAR),
            selectedMonth.get(Calendar.MONTH),
            selectedMonth.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateMonthLabel() {
        tvMonthLabel.text = monthLabelFormat.format(selectedMonth.time).uppercase()
    }

    private fun loadAllData() {
        loadDashboardData()
        loadPaymentHistory()
    }

    private fun loadDashboardData() {
        swipeRefresh.isRefreshing = true
        api.getDashboard().enqueue(object : Callback<CustomerDashboardResponse> {
            override fun onResponse(
                call: Call<CustomerDashboardResponse>,
                response: Response<CustomerDashboardResponse>
            ) {
                swipeRefresh.isRefreshing = false
                if (!response.isSuccessful) {
                    showToast(getString(R.string.server_error_code, response.code()))
                    return
                }

                val dashboard = response.body() ?: return
                val summary = dashboard.summary
                tvCurrentDue.text = getString(R.string.format_currency, summary.outstandingDue)
                tvCurrentBalance.text = getString(R.string.format_currency, summary.walletBalance)
            }

            override fun onFailure(call: Call<CustomerDashboardResponse>, t: Throwable) {
                swipeRefresh.isRefreshing = false
                showToast(
                    getString(
                        R.string.network_error_message,
                        t.localizedMessage ?: getString(R.string.unable_to_reach_server)
                    )
                )
            }
        })
    }

    private fun loadPaymentHistory() {
        val userId = session.getUserId()
        if (userId == -1) return

        lifecycleScope.launch {
            try {
                val response = ApiClient.deliveryApi.getCustomerPaymentRecords(
                    customerId = userId,
                    month = selectedMonth.get(Calendar.MONTH) + 1,
                    year = selectedMonth.get(Calendar.YEAR)
                ).awaitResponse()

                if (response.isSuccessful) {
                    val payments = response.body()?.payments.orEmpty()
                    historyAdapter.submitList(payments)
                    layoutEmptyHistory.visibility = if (payments.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                Log.e("CustomerPayment", "Error loading history", e)
            }
        }
    }

    private fun getAmount(): String? {
        val amount = etAmount.text.toString().trim()
        if (amount.isEmpty() || amount.toDoubleOrNull() == null || amount.toDouble() <= 0) {
            showToast(getString(R.string.enter_valid_amount))
            return null
        }
        return amount
    }

    private fun startGatewayPayment() {
        val amount = getAmount() ?: return

        // Format month as YYYY-MM to match backend expectations
        val monthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(selectedMonth.time)

        val paymentData = mapOf(
            "amount" to amount,
            "payment_for" to "GENERAL",
            "month" to monthStr
        )

        btnUpi.showLoading(true, "Initiating...")
        api.initiateGatewayPayment(paymentData).enqueue(object : Callback<PaymentGatewayInitResponse> {
            override fun onResponse(
                call: Call<PaymentGatewayInitResponse>,
                response: Response<PaymentGatewayInitResponse>
            ) {
                if (!isAdded) return
                btnUpi.showLoading(false)

                val body = response.body()
                if (response.isSuccessful && body?.success == true && body.canStartUpiPayment()) {
                    // Update history immediately if provided in response
                    body.paymentHistory?.let {
                        historyAdapter.submitList(it)
                        layoutEmptyHistory.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
                    }
                    showUpiPaymentDialog(body)
                } else {
                    showToast(body?.error ?: body?.message ?: getString(R.string.payment_update_failed), Toast.LENGTH_LONG)
                }
            }

            override fun onFailure(call: Call<PaymentGatewayInitResponse>, t: Throwable) {
                if (!isAdded) return
                btnUpi.showLoading(false)

                showToast(
                    getString(
                        R.string.failed_to_update_payment,
                        t.localizedMessage ?: getString(R.string.unable_to_reach_server)
                    ),
                    Toast.LENGTH_LONG
                )
            }
        })
    }

    private fun PaymentGatewayInitResponse.canStartUpiPayment(): Boolean {
        return amount != null && (!upiUri.isNullOrBlank() || !qrPayload.isNullOrBlank())
    }

    private fun showUpiPaymentDialog(payment: PaymentGatewayInitResponse) {
        val note = payment.paymentNote.orEmpty()
        val updateMessage = payment.recordUpdateMessage ?: getString(R.string.payment_updated_successfully)
        val message = buildString {
            append("Amount: Rs.").append(String.format("%.2f", payment.amount ?: 0.0)).append("\n")
            append("UPI ID: ").append(payment.upiId ?: "9392890375@axl")
            if (note.isNotBlank()) append("\nNote: ").append(note)
            append("\n\n").append(updateMessage)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Pay Now with UPI")
            .setMessage(message)
            .setPositiveButton("Open PhonePe / UPI app") { _, _ -> openUpiApp(payment) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openUpiApp(payment: PaymentGatewayInitResponse) {
        val upiUri = payment.upiUri ?: payment.qrPayload ?: return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(upiUri)))
            showUtrEntryDialog(payment)
        } catch (_: Exception) {
            showToast(getString(R.string.no_upi_app_found), Toast.LENGTH_LONG)
        }
    }

    private fun showUtrEntryDialog(payment: PaymentGatewayInitResponse) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.delivery_utr_entry, null)
        val etUtr = dialogView.findViewById<EditText>(R.id.etUtr)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm Payment")
            .setMessage("Please enter the 12-digit Transaction ID (UTR) from your UPI app after payment.")
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Confirm Recording") { _, _ ->
                val utr = etUtr.text.toString().trim()
                if (utr.length < 6) {
                    showToast("Please enter a valid Transaction ID")
                    showUtrEntryDialog(payment)
                } else {
                    confirmPaymentWithUtr(payment, utr)
                }
            }
            .setNegativeButton("I'll do it later") { _, _ ->
                etAmount.text.clear()
                loadAllData()
            }
            .show()
    }

    private fun confirmPaymentWithUtr(payment: PaymentGatewayInitResponse, utr: String) {
        // Format month as YYYY-MM to match backend expectations
        val monthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(selectedMonth.time)

        val recordData = mutableMapOf<String, Any?>(
            "amount" to (payment.amount ?: 0.0).toString(),
            "transaction_id" to utr,
            "payment_order_id" to payment.paymentOrderId,
            "method" to "UPI",
            "payment_for" to (payment.paymentFor ?: "GENERAL"),
            "month" to monthStr
        )
        payment.billId?.let { recordData["bill_id"] = it }

        btnUpi.showLoading(true, "Recording...")
        api.recordCustomerPayment(recordData).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (!isAdded) return
                btnUpi.showLoading(false)

                if (response.isSuccessful) {
                    val body = response.body()
                    showToast(getString(R.string.payment_updated_successfully), Toast.LENGTH_LONG)
                    etAmount.text.clear()

                    // Update UI with new balance and history if available
                    body?.new_balance?.let {
                        tvCurrentDue.text = getString(R.string.format_currency, it.toDoubleOrNull() ?: 0.0)
                    }
                    body?.paymentHistory?.let {
                        historyAdapter.submitList(it)
                        layoutEmptyHistory.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
                    }
                    loadAllData() // Refresh everything just in case
                } else {
                    showToast("Failed to record payment on server: ${response.code()}", Toast.LENGTH_LONG)
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                if (!isAdded) return
                btnUpi.showLoading(false)
                showToast("Error recording payment: ${t.localizedMessage}", Toast.LENGTH_LONG)
            }
        })
    }

    private fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        val appContext = context ?: return
        Toast.makeText(appContext, message, duration).show()
    }
}
