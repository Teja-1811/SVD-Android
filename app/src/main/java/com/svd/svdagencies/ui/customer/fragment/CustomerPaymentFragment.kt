package com.svd.svdagencies.ui.customer.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.api.customer.CustomerApi
import com.svd.svdagencies.data.model.customer.CustomerDashboardResponse
import com.svd.svdagencies.data.model.customer.GenericResponse
import com.svd.svdagencies.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CustomerPaymentFragment : Fragment() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var etAmount: EditText
    private lateinit var btnUpi: Button

    private lateinit var tvCurrentDue: TextView
    private lateinit var tvCurrentBalance: TextView

    // 🔑 UPI Details
    private val upiId = "svdmilkagency@ptyes"
    private val payeeName = "Sri Vijaya Durga Milk Agencies"

    private lateinit var api: CustomerApi
    private lateinit var session: SessionManager
    private var userId: Int = -1

    // 🔄 UPI Result Launcher
    private val upiLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                handleUpiResponse(result.data?.getStringExtra("response"))
            } else {
                context?.let {
                    Toast.makeText(it, "Payment cancelled", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.customer_payment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔗 Bind Views
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        etAmount = view.findViewById(R.id.etPayAmount)
        btnUpi = view.findViewById(R.id.btnPayUpi)
        tvCurrentDue = view.findViewById(R.id.tvCurrentDue)
        tvCurrentBalance = view.findViewById(R.id.tvCurrentBalance)

        // Session
        session = SessionManager(requireContext())
        userId = session.getUserId()

        if (userId == -1) {
            Toast.makeText(requireContext(), "Session expired", Toast.LENGTH_SHORT).show()
            return
        }

        // API
        api = ApiClient.retrofit.create(CustomerApi::class.java)

        // Initial load
        loadDashboardData()

        // 🎯 Actions
        btnUpi.setOnClickListener { startUpiAppPayment() }
        
        swipeRefresh.setOnRefreshListener {
            loadDashboardData()
        }
    }

    private fun loadDashboardData() {
        swipeRefresh.isRefreshing = true
        api.getDashboard(userId).enqueue(object : Callback<CustomerDashboardResponse> {
            override fun onResponse(
                call: Call<CustomerDashboardResponse>,
                response: Response<CustomerDashboardResponse>
            ) {
                val context = context ?: return
                swipeRefresh.isRefreshing = false
                if (!response.isSuccessful) {
                    Toast.makeText(context, "Server error: ${response.code()}", Toast.LENGTH_LONG).show()
                    return
                }

                val customer = response.body() ?: return
                val balance = customer.balance

                if (balance > 0) {
                    tvCurrentDue.text = String.format("₹%.2f", balance)
                    tvCurrentBalance.text = "₹0.00"
                } else {
                    tvCurrentDue.text = "₹0.00"
                    tvCurrentBalance.text = String.format("₹%.2f", -balance)
                }
            }

            override fun onFailure(call: Call<CustomerDashboardResponse>, t: Throwable) {
                val context = context ?: return
                swipeRefresh.isRefreshing = false
                Toast.makeText(context, "Network error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ------------------ VALIDATE AMOUNT ------------------
    private fun getAmount(): String? {
        val amount = etAmount.text.toString().trim()
        if (amount.isEmpty() || amount.toDoubleOrNull() == null || amount.toDouble() <= 0) {
            Toast.makeText(requireContext(), "Enter valid amount", Toast.LENGTH_SHORT).show()
            return null
        }
        return amount
    }

    // ------------------ UPI APP PAYMENT ------------------
    private fun startUpiAppPayment() {
        val amount = getAmount() ?: return

        val uri = Uri.parse("upi://pay").buildUpon()
            .appendQueryParameter("pa", upiId)
            .appendQueryParameter("pn", payeeName)
            .appendQueryParameter("tn", "Customer Payment")
            .appendQueryParameter("am", amount)
            .appendQueryParameter("cu", "INR")
            .build()

        val intent = Intent(Intent.ACTION_VIEW, uri)
        val packageManager = activity?.packageManager ?: return
        if (intent.resolveActivity(packageManager) != null) {
            upiLauncher.launch(intent)
        } else {
            Toast.makeText(requireContext(), "No UPI app found", Toast.LENGTH_SHORT).show()
        }
    }

    // ------------------ HANDLE UPI RESPONSE ------------------
    private fun handleUpiResponse(response: String?) {
        val context = context ?: return
        if (response == null) {
            Toast.makeText(context, "Payment cancelled", Toast.LENGTH_SHORT).show()
            return
        }

        val params = response.split("&").associate { 
            val p = it.split("=")
            p[0].lowercase() to p.getOrElse(1) { "" }
        }

        val status = params["status"]
        val txnId = params["txnref"] ?: params["tr"] ?: ""

        if (status.equals("success", true)) {
            val amount = getAmount() ?: return
            sendPaymentDataToBackend(txnId, amount)
        } else {
            Toast.makeText(context, "Payment failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendPaymentDataToBackend(txnId: String, amount: String) {
        val paymentData = mapOf(
            "user_id" to userId.toString(),
            "amount" to amount,
            "transaction_id" to txnId,
            "status" to "success"
        )

        api.recordCustomerPayment(paymentData).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (isAdded) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Payment updated successfully", Toast.LENGTH_LONG).show()
                        etAmount.text.clear()
                        loadDashboardData() // Refresh balances
                    } else {
                        Toast.makeText(context, "Payment updated failed on server", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                if (isAdded) {
                    Toast.makeText(context, "Failed to update payment: ${t.message}", Toast.LENGTH_LONG).show()
                }
            }
        })
    }
}