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
    private var shareCall: retrofit2.Call<PublicTrackingShare>? = null
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
                if (uri?.scheme == "svd-share" && uri.host == "location") {
                    val agentId = uri.getQueryParameter("agent_id")?.toIntOrNull()
                    if (agentId != null && agentId > 0) shareAgent(agentId)
                    return true
                }
                if (uri?.scheme == "https" && uri.host == "www.openstreetmap.org" && uri.path == "/copyright") {
                    runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri)) }
                }
                return true
            }
        }
        loadDataWithBaseURL("https://svdagencies.shop/embedded-agent-map/", context.assets.open("agent-map.html").bufferedReader().use { it.readText() }, "text/html", "UTF-8", null)
    }
    private fun shareAgent(agentId: Int) {
        shareCall?.cancel()
        val call = LiveLocationNetwork.api.share(agentId)
        shareCall = call
        android.widget.Toast.makeText(context, "Creating live tracking link…", android.widget.Toast.LENGTH_SHORT).show()
        call.enqueue(object : retrofit2.Callback<PublicTrackingShare> {
            override fun onResponse(call: retrofit2.Call<PublicTrackingShare>, response: retrofit2.Response<PublicTrackingShare>) {
                if (shareCall !== call) return
                shareCall = null
                val activity = context as? android.app.Activity ?: return
                if (activity.isFinishing || activity.isDestroyed || !activity.hasWindowFocus()) return
                val body = response.body()
                val uri = body?.url?.let { android.net.Uri.parse(it) }
                if (!response.isSuccessful || uri?.scheme != "https" || uri.host !in setOf("svdagencies.shop", "www.svdagencies.shop") || uri.path?.startsWith("/milk_agency/track/") != true) {
                    android.widget.Toast.makeText(context, "Could not create tracking link. Try again.", android.widget.Toast.LENGTH_LONG).show(); return
                }
                val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, "SVD live delivery tracking — valid until ${body!!.expires_at}\n${body.url}")
                }
                runCatching { context.startActivity(android.content.Intent.createChooser(send, "Share live tracking link")) }
            }
            override fun onFailure(call: retrofit2.Call<PublicTrackingShare>, error: Throwable) {
                if (shareCall !== call) return
                shareCall = null
                android.widget.Toast.makeText(context, "Could not create tracking link. Check your connection.", android.widget.Toast.LENGTH_LONG).show()
            }
        })
    }
    override fun destroy() { shareCall?.cancel(); shareCall = null; super.destroy() }
    fun update(positions: AgentPositions) { data = Gson().toJson(positions); failed = false; flush() }
    fun offline() { failed = true; flush() }
    private fun flush() {
        if (!ready) return
        data?.let { evaluateJavascript("window.SvdAgentMap && window.SvdAgentMap.update($it);", null) }
        if (failed) evaluateJavascript("window.SvdAgentMap && window.SvdAgentMap.offline();", null)
    }
}
