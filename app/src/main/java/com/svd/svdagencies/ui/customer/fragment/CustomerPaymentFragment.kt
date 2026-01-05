package com.svd.svdagencies.ui.customer.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.api.customer.CustomerApi
import com.svd.svdagencies.data.model.customer.CustomerDashboardResponse
import com.svd.svdagencies.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CustomerPaymentFragment : Fragment() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var etAmount: EditText
    private lateinit var btnUpi: Button
    private lateinit var btnQr: Button

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
                Toast.makeText(requireActivity(), "Payment cancelled", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_customer_payment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔗 Bind Views
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        etAmount = view.findViewById(R.id.etPayAmount)
        btnUpi = view.findViewById(R.id.btnPayUpi)
        btnQr = view.findViewById(R.id.btnScanQr)
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
        btnQr.setOnClickListener { showQrPayment() }
        
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
                swipeRefresh.isRefreshing = false
                if (!response.isSuccessful) {
                    Toast.makeText(requireContext(), "Server error: ${response.code()}", Toast.LENGTH_LONG).show()
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
                swipeRefresh.isRefreshing = false
                Toast.makeText(requireContext(), "Network error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ------------------ VALIDATE AMOUNT ------------------
    private fun getAmount(): String? {
        val amount = etAmount.text.toString().trim()
        if (amount.isEmpty() || amount.toDoubleOrNull() == null || amount.toDouble() <= 0) {
            Toast.makeText(requireActivity(), "Enter valid amount", Toast.LENGTH_SHORT).show()
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
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            upiLauncher.launch(intent)
        } else {
            Toast.makeText(requireActivity(), "No UPI app found", Toast.LENGTH_SHORT).show()
        }
    }

    // ------------------ QR PAYMENT ------------------
    private fun showQrPayment() {
        val amount = getAmount() ?: return

        val dialog = Dialog(requireActivity())
        dialog.setContentView(R.layout.dialog_upi_qr)

        val imgQr = dialog.findViewById<ImageView>(R.id.imgQr)
        imgQr.setImageBitmap(generateUpiQr(amount))

        dialog.show()
    }

    private fun generateUpiQr(amount: String): Bitmap {
        val upiUri = "upi://pay?pa=$upiId&pn=$payeeName&am=$amount&cu=INR"

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(upiUri, BarcodeFormat.QR_CODE, 600, 600)

        val bitmap = Bitmap.createBitmap(600, 600, Bitmap.Config.RGB_565)
        for (x in 0 until 600) {
            for (y in 0 until 600) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    // ------------------ HANDLE UPI RESPONSE ------------------
    private fun handleUpiResponse(response: String?) {
        if (response == null) {
            Toast.makeText(requireActivity(), "Payment cancelled", Toast.LENGTH_SHORT).show()
            return
        }

        val params = response.split("&").associate { 
            val p = it.split("=")
            p[0].lowercase() to p.getOrElse(1) { "" }
        }

        val status = params["status"]

        if (status.equals("success", true)) {
            Toast.makeText(requireActivity(), "Payment successful", Toast.LENGTH_SHORT).show()

            // 🔥 After this → call backend API to update balance & due
            // sendPaymentToBackend(txnId, amount)

        } else {
            Toast.makeText(requireActivity(), "Payment failed", Toast.LENGTH_SHORT).show()
        }
    }
}
