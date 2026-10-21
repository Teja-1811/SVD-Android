package com.svd.svdagencies.ui.customer.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.svd.svdagencies.utils.UpiPaymentLauncher
import com.svd.svdagencies.utils.PaymentConfig
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
import com.svd.svdagencies.utils.WhiteMaterialAlertDialogBuilder as MaterialAlertDialogBuilder
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

    private var paymentLocked = false
    private var awaitingUpiReturn = false
    private var attemptInProgress = false
    private var activeAttempt: PaymentGatewayInitResponse? = null
    private var supportButton: com.google.android.material.button.MaterialButton? = null
    private var verificationLabel: TextView? = null
    private val verificationHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val verificationPoll = object : Runnable {
        override fun run() {
            if (view != null && isResumed && !isHidden) {
                loadAttemptStatus()
                verificationHandler.postDelayed(this, 30_000L)
            }
        }
    }
    private lateinit var api: CustomerApi
    private lateinit var session: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(if (SessionManager(requireContext()).getRole() == com.svd.svdagencies.utils.UserRole.USER) R.layout.user_payment else R.layout.customer_payment, container, false)

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
        verificationLabel = TextView(requireContext()).apply {
            text = "Payment verification takes 5 minutes. If it has not updated after 5 minutes, contact us on WhatsApp with your registered number."
            setPadding(0, 16, 0, 8)
        }
        supportButton = com.google.android.material.button.MaterialButton(requireContext()).apply {
            text = "Payment not updated? WhatsApp support"
            isAllCaps = false
            visibility = View.GONE
        }
        (btnUpi.parent as? android.widget.LinearLayout)?.let { parent ->
            parent.addView(verificationLabel, parent.indexOfChild(btnUpi) + 1)
            parent.addView(supportButton, parent.indexOfChild(btnUpi) + 2)
        }
        if ((arguments?.getInt("subscription_id", -1) ?: -1) != -1) {
            etAmount.setText(String.format(Locale.US, "%.2f", arguments?.getDouble("subscription_amount") ?: 0.0))
            verificationLabel?.text = "${arguments?.getString("subscription_name").orEmpty()} subscription payment. " + verificationLabel?.text
        }
        checkoutAmount()?.let { etAmount.setText(String.format(Locale.US, "%.2f", it)) }
        applyPaymentLock()
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

    override fun onResume() {
        super.onResume()
        if (awaitingUpiReturn) {
            awaitingUpiReturn = false
            paymentLocked = false
            applyPaymentLock()
        }
        if (::api.isInitialized && view != null) {
            verificationHandler.removeCallbacks(verificationPoll)
            verificationHandler.post(verificationPoll)
        }
    }

    override fun onPause() {
        verificationHandler.removeCallbacks(verificationPoll)
        super.onPause()
    }

    override fun onDestroyView() {
        verificationHandler.removeCallbacks(verificationPoll)
        supportButton = null
        verificationLabel = null
        super.onDestroyView()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        verificationHandler.removeCallbacks(verificationPoll)
        if (!hidden && ::api.isInitialized && view != null) verificationHandler.post(verificationPoll)
    }

    private fun checkoutAmount(): Double? = when {
        (arguments?.getInt("subscription_id", -1) ?: -1) != -1 -> arguments?.getDouble("subscription_amount")
        (arguments?.getInt("order_id", -1) ?: -1) != -1 -> arguments?.getDouble("order_amount")
        else -> null
    }

    private fun applyPaymentLock() {
        if (!::btnUpi.isInitialized || view == null) return
        btnUpi.isEnabled = !paymentLocked && !attemptInProgress
        etAmount.isEnabled = !paymentLocked && !attemptInProgress && checkoutAmount() == null
        if (paymentLocked && !attemptInProgress) btnUpi.text = "Opening payment app..."
        else if (!attemptInProgress) btnUpi.text = "Pay with UPI"
    }

    private fun loadAttemptStatus() {
        val screen = view ?: return
        api.getPaymentAttemptStatus(activeAttempt?.paymentOrderId, arguments?.getInt("subscription_id", -1)?.takeIf { it != -1 }).enqueue(object : Callback<PaymentGatewayInitResponse> {
            override fun onResponse(call: Call<PaymentGatewayInitResponse>, response: Response<PaymentGatewayInitResponse>) {
                if (view !== screen || attemptInProgress) return
                val body = response.body() ?: return
                if (!response.isSuccessful) return
                activeAttempt = body.takeIf { !it.paymentOrderId.isNullOrBlank() }
                body.verificationMessage?.let { verificationLabel?.text = it }
                if (body.verificationStatus == "success") verificationLabel?.text = "Payment verified successfully."
                supportButton?.visibility = if (body.supportAvailable && !body.supportWhatsappUrl.isNullOrBlank()) View.VISIBLE else View.GONE
                supportButton?.setOnClickListener {
                    val url = body.supportWhatsappUrl ?: return@setOnClickListener
                    runCatching { startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
                        .onFailure { showToast("Unable to open WhatsApp support.") }
                }
                applyPaymentLock()
            }
            override fun onFailure(call: Call<PaymentGatewayInitResponse>, error: Throwable) = Unit
        })
    }

    private fun loadAllData() {
        loadAttemptStatus()
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
                val outstandingVisibility = if (dashboard.customer.allowCredit) View.VISIBLE else View.GONE
                view?.findViewById<View>(R.id.paymentOutstandingCard)?.visibility = outstandingVisibility
                view?.findViewById<View>(R.id.paymentBalanceDivider)?.visibility = outstandingVisibility
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
        if (paymentLocked || attemptInProgress) return
        val amount = getAmount() ?: return
        paymentLocked = true
        attemptInProgress = true
        applyPaymentLock()

        // Format month as YYYY-MM to match backend expectations
        val monthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(selectedMonth.time)

        val paymentData = mutableMapOf(
            "amount" to amount,
            "payment_for" to "GENERAL",
            "month" to monthStr
        )

        arguments?.getInt("subscription_id", -1)?.takeIf { it != -1 }?.let {
            paymentData["subscription_id"] = it.toString()
        }
        arguments?.getInt("order_id", -1)?.takeIf { it != -1 }?.let {
            paymentData["order_id"] = it.toString()
        }
        btnUpi.showLoading(true, "Initiating...")
        api.initiateGatewayPayment(paymentData).enqueue(object : Callback<PaymentGatewayInitResponse> {
            override fun onResponse(
                call: Call<PaymentGatewayInitResponse>,
                response: Response<PaymentGatewayInitResponse>
            ) {
                if (!isAdded) return
                btnUpi.showLoading(false)
                attemptInProgress = false
                applyPaymentLock()

                val body = response.body()
                if (response.isSuccessful && body?.success == true && body.canStartUpiPayment()) {
                    // Update history immediately if provided in response
                    body.paymentHistory?.let {
                        historyAdapter.submitList(it)
                        layoutEmptyHistory.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
                    }
                    activeAttempt = body
                    paymentLocked = true
                    applyPaymentLock()
                    showUpiPaymentDialog(body)
                } else {
                    paymentLocked = false
                    applyPaymentLock()
                    val error = runCatching { com.google.gson.JsonParser.parseString(response.errorBody()?.string()).asJsonObject }.getOrNull()
                    showToast(error?.get("message")?.asString ?: error?.get("error")?.asString ?: body?.error ?: body?.message ?: getString(R.string.payment_update_failed), Toast.LENGTH_LONG)
                }
            }

            override fun onFailure(call: Call<PaymentGatewayInitResponse>, t: Throwable) {
                if (!isAdded) return
                btnUpi.showLoading(false)
                attemptInProgress = false
                paymentLocked = false
                applyPaymentLock()
                loadAttemptStatus()

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
        val content = layoutInflater.inflate(R.layout.dialog_upi_payment, null)
        content.findViewById<TextView>(R.id.upiDialogAmount).text =
            java.text.NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(payment.amount ?: 0.0)
        content.findViewById<TextView>(R.id.upiDialogPayee).text = PaymentConfig.PAYEE_NAME
        content.findViewById<TextView>(R.id.upiDialogId).text = PaymentConfig.UPI_ID
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(content)
            .setOnCancelListener { releasePaymentButton() }
            .create()
        content.findViewById<MaterialButton>(R.id.upiDialogCancel).setOnClickListener {
            dialog.dismiss()
            releasePaymentButton()
        }
        content.findViewById<MaterialButton>(R.id.upiDialogPay).setOnClickListener { button ->
            button.isEnabled = false
            dialog.dismiss()
            openUpiApp(payment)
        }
        dialog.show()
    }

    private fun releasePaymentButton() {
        paymentLocked = false
        applyPaymentLock()
    }

    private fun openUpiApp(payment: PaymentGatewayInitResponse) {
        val upiUri = payment.upiUri ?: payment.qrPayload ?: run { releasePaymentButton(); return }
        UpiPaymentLauncher.open(requireContext(), upiUri,
            onCancelled = { releasePaymentButton() },
            onLaunched = {
                awaitingUpiReturn = true
                paymentLocked = true
                applyPaymentLock()
            })
    }

    private fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        val appContext = context ?: return
        Toast.makeText(appContext, message, duration).show()
    }
}
