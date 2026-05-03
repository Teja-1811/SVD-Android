package com.svd.svdagencies.ui.customer.fragment

import android.content.Intent
import android.os.Bundle
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.api.customer.CustomerApi
import com.svd.svdagencies.data.model.customer.CustomerDashboardResponse
import com.svd.svdagencies.data.model.customer.PaymentGatewayInitResponse
import com.svd.svdagencies.data.model.customer.PaymentGatewayResultResponse
import com.svd.svdagencies.ui.customer.PaytmNativeCheckoutActivity
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

    private lateinit var api: CustomerApi
    private lateinit var session: SessionManager

    private val paytmLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val paymentOrderId = result.data?.getStringExtra(PaytmNativeCheckoutActivity.EXTRA_PAYMENT_ORDER_ID).orEmpty()
            if (result.resultCode == Activity.RESULT_OK && paymentOrderId.isNotBlank()) {
                confirmGatewayPayment(paymentOrderId)
            } else {
                showToast(getString(R.string.payment_cancelled))
                loadDashboardData()
            }
        }

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

        session = SessionManager(requireContext())
        if (session.getUserId() == -1) {
            showToast(getString(R.string.session_expired))
            return
        }

        api = ApiClient.customerApi

        btnUpi.setOnClickListener { startGatewayPayment() }
        swipeRefresh.setOnRefreshListener { loadDashboardData() }

        loadDashboardData()
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

        val paymentData = mapOf(
            "amount" to amount,
            "payment_for" to "GENERAL"
        )

        btnUpi.isEnabled = false
        api.initiateGatewayPayment(paymentData).enqueue(object : Callback<PaymentGatewayInitResponse> {
            override fun onResponse(
                call: Call<PaymentGatewayInitResponse>,
                response: Response<PaymentGatewayInitResponse>
            ) {
                if (!isAdded) return
                btnUpi.isEnabled = true

                val body = response.body()
                if (response.isSuccessful && body?.success == true && body.canStartCheckout()) {
                    paytmLauncher.launch(
                        Intent(requireContext(), PaytmNativeCheckoutActivity::class.java).apply {
                            putExtra(PaytmNativeCheckoutActivity.EXTRA_PAYMENT_ORDER_ID, body.paymentOrderId)
                            putExtra(PaytmNativeCheckoutActivity.EXTRA_TXN_TOKEN, body.txnToken)
                            putExtra(PaytmNativeCheckoutActivity.EXTRA_MID, body.mid)
                            putExtra(PaytmNativeCheckoutActivity.EXTRA_AMOUNT, "%.2f".format(body.amount ?: amount.toDouble()))
                            putExtra(PaytmNativeCheckoutActivity.EXTRA_CALLBACK_URL, body.callbackUrl)
                            putExtra(PaytmNativeCheckoutActivity.EXTRA_CHECKOUT_HOST, body.checkoutHost)
                        }
                    )
                } else {
                    showToast(body?.error ?: getString(R.string.payment_update_failed), Toast.LENGTH_LONG)
                }
            }

            override fun onFailure(call: Call<PaymentGatewayInitResponse>, t: Throwable) {
                if (!isAdded) return
                btnUpi.isEnabled = true

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

    private fun confirmGatewayPayment(paymentOrderId: String) {
        val paymentData = mapOf("payment_order_id" to paymentOrderId)
        btnUpi.isEnabled = false
        api.confirmGatewayPayment(paymentData).enqueue(object : Callback<PaymentGatewayResultResponse> {
            override fun onResponse(
                call: Call<PaymentGatewayResultResponse>,
                response: Response<PaymentGatewayResultResponse>
            ) {
                if (!isAdded) return
                btnUpi.isEnabled = true

                val body = response.body()
                if (response.isSuccessful && body?.isSuccess == true) {
                    showToast(getString(R.string.payment_updated_successfully), Toast.LENGTH_LONG)
                    etAmount.text.clear()
                    loadDashboardData()
                } else {
                    showToast(body?.error ?: getString(R.string.payment_failed), Toast.LENGTH_LONG)
                    loadDashboardData()
                }
            }

            override fun onFailure(call: Call<PaymentGatewayResultResponse>, t: Throwable) {
                if (!isAdded) return
                btnUpi.isEnabled = true
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

    private fun PaymentGatewayInitResponse.canStartCheckout(): Boolean {
        return !paymentOrderId.isNullOrBlank() &&
            !txnToken.isNullOrBlank() &&
            !mid.isNullOrBlank() &&
            !callbackUrl.isNullOrBlank() &&
            !checkoutHost.isNullOrBlank() &&
            amount != null
    }

    private fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        val appContext = context ?: return
        Toast.makeText(appContext, message, duration).show()
    }
}
