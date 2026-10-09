package com.svd.svdagencies.ui.customer

import android.app.Activity
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import com.svd.svdagencies.base.BaseActivity

class PhonePeCheckoutActivity : BaseActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val paymentOrderId = intent.getStringExtra(EXTRA_PAYMENT_ORDER_ID).orEmpty()
        val redirectUrl = intent.getStringExtra(EXTRA_REDIRECT_URL).orEmpty()
        if (paymentOrderId.isBlank() || redirectUrl.isBlank()) {
            finishWithResult(Activity.RESULT_CANCELED, paymentOrderId)
            return
        }

        webView = WebView(this)
        val progress = ProgressBar(this)
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.WHITE)
            addView(
                webView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
            addView(
                progress,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
        }
        setContentView(root)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString().orEmpty()
                if (isReturnUrl(url)) {
                    finishWithResult(Activity.RESULT_OK, paymentOrderId)
                    return true
                }
                return false
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                val target = url.orEmpty()
                if (isReturnUrl(target)) {
                    finishWithResult(Activity.RESULT_OK, paymentOrderId)
                    return true
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progress.visibility = android.view.View.GONE
            }
        }
        webView.loadUrl(redirectUrl)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (::webView.isInitialized && webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finishWithResult(RESULT_CANCELED, intent.getStringExtra(EXTRA_PAYMENT_ORDER_ID).orEmpty())
                }
            }
        })
    }

    private fun isReturnUrl(url: String): Boolean {
        return url.contains("/api/customer/payment/phonepe/result/", ignoreCase = true) ||
            url.contains("payment_order_id=", ignoreCase = true)
    }

    private fun finishWithResult(resultCode: Int, paymentOrderId: String) {
        setResult(
            resultCode,
            Intent().putExtra(EXTRA_PAYMENT_ORDER_ID, paymentOrderId)
        )
        finish()
    }

    companion object {
        const val EXTRA_PAYMENT_ORDER_ID = "payment_order_id"
        const val EXTRA_REDIRECT_URL = "redirect_url"
    }
}
