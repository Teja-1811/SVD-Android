package com.svd.svdagencies.ui.customer

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.customer.PaymentGatewayInitResponse
import com.svd.svdagencies.utils.WhiteMaterialAlertDialogBuilder
import com.svd.svdagencies.utils.UpiPaymentLauncher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.awaitResponse
import java.text.NumberFormat
import java.util.Locale

object WalletTopUpDialog {
    fun show(fragment: Fragment, required: Double, shortfall: Double) {
        val screen = fragment.view ?: return
        val money = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        val alert = WhiteMaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle("Add money to place order")
            .setMessage("Order total: ${money.format(required)}\nAdd ${money.format(shortfall)} to your wallet.")
            .setNegativeButton("Not now", null).setPositiveButton("Pay", null).create()
        alert.setOnShowListener {
            alert.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener { button ->
                button.isEnabled = false
                val call = ApiClient.customerApi.initiateGatewayPayment(mapOf("amount" to String.format(Locale.US,"%.2f",shortfall), "payment_for" to "GENERAL"))
                fragment.viewLifecycleOwner.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
                    override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) { call.cancel(); alert.dismiss() }
                })
                call.enqueue(object : Callback<PaymentGatewayInitResponse> {
                    override fun onResponse(call: Call<PaymentGatewayInitResponse>, response: Response<PaymentGatewayInitResponse>) {
                        if (!fragment.isAdded || fragment.view !== screen || call.isCanceled) return
                        button.isEnabled = true
                        val payment = response.body()
                        val uri = payment?.upiUri ?: payment?.qrPayload
                        if (!response.isSuccessful || payment?.success != true || uri.isNullOrBlank()) {
                            alert.setMessage("Unable to start payment. Please try again."); return
                        }
                        alert.dismiss()
                        val content = fragment.layoutInflater.inflate(com.svd.svdagencies.R.layout.dialog_upi_payment,null)
                        content.findViewById<android.widget.TextView>(com.svd.svdagencies.R.id.upiDialogAmount).text = money.format(shortfall)
                        content.findViewById<android.widget.TextView>(com.svd.svdagencies.R.id.upiDialogPayee).text = com.svd.svdagencies.utils.PaymentConfig.PAYEE_NAME
                        content.findViewById<android.widget.TextView>(com.svd.svdagencies.R.id.upiDialogId).text = com.svd.svdagencies.utils.PaymentConfig.UPI_ID
                        val init = WhiteMaterialAlertDialogBuilder(fragment.requireContext()).setView(content).create()
                        content.findViewById<android.view.View>(com.svd.svdagencies.R.id.upiDialogCancel).setOnClickListener { init.dismiss() }
                        content.findViewById<android.view.View>(com.svd.svdagencies.R.id.upiDialogPay).setOnClickListener { pay ->
                            pay.isEnabled = false
                            UpiPaymentLauncher.open(fragment.requireContext(), uri,
                                onCancelled = { pay.isEnabled = true },
                                onLaunched = {
                                    init.dismiss()
                                    val waiting = WhiteMaterialAlertDialogBuilder(fragment.requireContext())
                                        .setTitle("Waiting for wallet update")
                                        .setMessage("Admin will verify your payment. Your order items remain here.")
                                        .setPositiveButton("Close",null).create()
                                    waiting.show()
                                    fragment.viewLifecycleOwner.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
                                        override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) { waiting.dismiss() }
                                    })
                                    val job = fragment.viewLifecycleOwner.lifecycleScope.launch {
                                        repeat(60) {
                                            delay(5000)
                                            if (!fragment.isAdded || fragment.view !== screen) return@launch
                                            if (!fragment.isResumed || fragment.isHidden) return@repeat
                                            val result = try { ApiClient.customerApi.getDashboard().awaitResponse() }
                                                catch (e: kotlinx.coroutines.CancellationException) { throw e }
                                                catch (_: Exception) { return@repeat }
                                            if (result.isSuccessful && (result.body()?.summary?.walletBalance ?: 0.0) >= required) {
                                                waiting.setTitle("Wallet updated")
                                                waiting.setMessage("You can now place your order.")
                                                return@launch
                                            }
                                        }
                                        waiting.setMessage("Payment is still awaiting admin verification. You can retry placing the order after your wallet is updated.")
                                    }
                                    waiting.setOnDismissListener { job.cancel() }
                                })
                        }
                        fragment.viewLifecycleOwner.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
                            override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) { init.dismiss() }
                        })
                        init.show()
                    }
                    override fun onFailure(call: Call<PaymentGatewayInitResponse>, error: Throwable) {
                        if (!fragment.isAdded || fragment.view !== screen || call.isCanceled) return
                        button.isEnabled = true; alert.setMessage("Unable to start payment. Please try again.")
                    }
                })
            }
        }
        alert.show()
    }
}
