package com.svd.svdagencies.ui.customer

import androidx.fragment.app.Fragment
import com.svd.svdagencies.utils.showLoading
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
        show(fragment.requireActivity(), fragment.viewLifecycleOwner,
            { fragment.isAdded && fragment.view === screen }, { fragment.isResumed && !fragment.isHidden }, required, shortfall)
    }

    fun show(activity: androidx.fragment.app.FragmentActivity, required: Double, shortfall: Double) {
        show(activity, activity, { !activity.isFinishing && !activity.isDestroyed },
            { activity.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) }, required, shortfall)
    }

    private fun show(activity: androidx.fragment.app.FragmentActivity, owner: androidx.lifecycle.LifecycleOwner,
                     valid: () -> Boolean, active: () -> Boolean, required: Double, shortfall: Double) {
        val money = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        val alert = WhiteMaterialAlertDialogBuilder(activity)
            .setTitle("Add money to continue")
            .setMessage("Required wallet balance: ${money.format(required)}\nAdd ${money.format(shortfall)} to your wallet.")
            .setNegativeButton("Not now", null).setPositiveButton("Pay", null).create()
        alert.setOnShowListener {
            alert.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener { button ->
                button.isEnabled = false
                val payButton = button as android.widget.Button
                payButton.text = "Starting payment…"
                val pulse = android.animation.ObjectAnimator.ofFloat(button, "alpha", 1f, 0.55f, 1f).apply {
                    duration = 900; repeatCount = android.animation.ValueAnimator.INFINITE; start()
                }
                fun reset() { pulse.cancel(); button.alpha = 1f; button.isEnabled = true; payButton.text = "Pay" }
                val call = ApiClient.customerApi.initiateGatewayPayment(mapOf("amount" to String.format(Locale.US,"%.2f",shortfall), "payment_for" to "GENERAL"))
                owner.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
                    override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) { call.cancel(); reset(); alert.dismiss() }
                })
                call.enqueue(object : Callback<PaymentGatewayInitResponse> {
                    override fun onResponse(call: Call<PaymentGatewayInitResponse>, response: Response<PaymentGatewayInitResponse>) {
                        if (!valid() || call.isCanceled) return
                        reset()
                        val payment = response.body()
                        val uri = payment?.upiUri ?: payment?.qrPayload
                        if (!response.isSuccessful || payment?.success != true || uri.isNullOrBlank()) {
                            alert.setMessage("Unable to start payment. Please try again."); return
                        }
                        alert.dismiss()
                        val content = activity.layoutInflater.inflate(com.svd.svdagencies.R.layout.dialog_upi_payment,null)
                        content.findViewById<android.widget.TextView>(com.svd.svdagencies.R.id.upiDialogAmount).text = money.format(shortfall)
                        content.findViewById<android.widget.TextView>(com.svd.svdagencies.R.id.upiDialogPayee).text = com.svd.svdagencies.utils.PaymentConfig.PAYEE_NAME
                        content.findViewById<android.widget.TextView>(com.svd.svdagencies.R.id.upiDialogId).text = com.svd.svdagencies.utils.PaymentConfig.UPI_ID
                        val init = WhiteMaterialAlertDialogBuilder(activity).setView(content).create()
                        content.findViewById<android.view.View>(com.svd.svdagencies.R.id.upiDialogCancel).setOnClickListener { init.dismiss() }
                        content.findViewById<com.google.android.material.button.MaterialButton>(com.svd.svdagencies.R.id.upiDialogPay).setOnClickListener { view ->
                            val pay = view as com.google.android.material.button.MaterialButton
                            pay.showLoading(true, "Opening UPI…")
                            UpiPaymentLauncher.open(activity, uri,
                                onCancelled = { pay.showLoading(false) },
                                onLaunched = {
                                    init.dismiss()
                                    val waiting = WhiteMaterialAlertDialogBuilder(activity)
                                        .setTitle("Waiting for wallet update")
                                        .setMessage("Admin will verify your payment. Your selection remains on this screen.")
                                        .setPositiveButton("Close",null).create()
                                    waiting.show()
                                    owner.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
                                        override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) { waiting.dismiss() }
                                    })
                                    val job = owner.lifecycleScope.launch {
                                        repeat(60) {
                                            delay(5000)
                                            if (!valid()) return@launch
                                            if (!active()) return@repeat
                                            val result = try { ApiClient.customerApi.getDashboard().awaitResponse() }
                                                catch (e: kotlinx.coroutines.CancellationException) { throw e }
                                                catch (_: Exception) { return@repeat }
                                            if (result.isSuccessful && (result.body()?.summary?.walletBalance ?: 0.0) >= required) {
                                                waiting.setTitle("Wallet updated")
                                                waiting.setMessage("You can now complete your purchase.")
                                                return@launch
                                            }
                                        }
                                        waiting.setMessage("Payment is still awaiting admin verification. You can retry your purchase after your wallet is updated.")
                                    }
                                    waiting.setOnDismissListener { job.cancel() }
                                })
                        }
                        owner.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
                            override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) { init.dismiss() }
                        })
                        init.show()
                    }
                    override fun onFailure(call: Call<PaymentGatewayInitResponse>, error: Throwable) {
                        if (!valid() || call.isCanceled) return
                        reset(); alert.setMessage("Unable to start payment. Please try again.")
                    }
                })
            }
        }
        alert.show()
    }
}
