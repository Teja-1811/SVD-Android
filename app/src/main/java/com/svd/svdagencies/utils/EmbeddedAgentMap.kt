package com.svd.svdagencies.utils

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import com.google.gson.Gson

/** Bundled map UI; native code supplies positions without exposing auth tokens. */
@SuppressLint("SetJavaScriptEnabled")
class EmbeddedAgentMap(context: Context) : WebView(context) {
    private var ready = false
    private var data: String? = null
    private var failed = false
    init {
        settings.javaScriptEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.domStorageEnabled = false
        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.userAgentString += " SVD-AgentMap/1.0 (+https://svdagencies.shop)"
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) { ready = true; flush() }
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                // The map never navigates to a third-party site in this WebView.
                val uri = request?.url
                if (uri?.scheme == "https" && uri.host == "www.openstreetmap.org" && uri.path == "/copyright") {
                    runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri)) }
                }
                return true
            }
        }
        loadDataWithBaseURL("https://svdagencies.shop/embedded-agent-map/", context.assets.open("agent-map.html").bufferedReader().use { it.readText() }, "text/html", "UTF-8", null)
    }
    fun update(positions: AgentPositions) { data = Gson().toJson(positions); failed = false; flush() }
    fun offline() { failed = true; flush() }
    private fun flush() {
        if (!ready) return
        data?.let { evaluateJavascript("window.SvdAgentMap && window.SvdAgentMap.update($it);", null) }
        if (failed) evaluateJavascript("window.SvdAgentMap && window.SvdAgentMap.offline();", null)
    }
}
