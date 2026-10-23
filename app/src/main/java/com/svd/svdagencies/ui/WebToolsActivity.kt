package com.svd.svdagencies.ui

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.appbar.MaterialToolbar
import com.svd.svdagencies.base.BaseActivity
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.ui.auth.LoginActivity
import com.svd.svdagencies.utils.SessionManager

/** Embedded website tools; website cookies and forms use normal web authentication. */
class WebToolsActivity : BaseActivity() {
    private lateinit var web: WebView
    private var chooser: ValueCallback<Array<Uri>>? = null
    private val pickFile = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        chooser?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(it.resultCode, it.data))
        chooser = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SessionManager(this).getToken().isNullOrBlank()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        val base = Uri.parse(ApiClient.BASE_URL)
        val explicitUrl = intent.getStringExtra(EXTRA_URL)
        val initial = Uri.parse(explicitUrl ?: (ApiClient.BASE_URL.trimEnd('/') + "/" + intent.getStringExtra(EXTRA_PATH).orEmpty()))
        if (initial.scheme != "https" || initial.host.isNullOrBlank() || initial.userInfo != null ||
            (explicitUrl == null && (initial.host != base.host || initial.port != base.port))) {
            finish()
            return
        }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val toolbar = MaterialToolbar(this).apply {
            title = intent.getStringExtra(EXTRA_TITLE) ?: "Website tools"
            setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            navigationContentDescription = "Back"
            setNavigationOnClickListener { goBack() }
            menu.add("Refresh").setOnMenuItemClickListener { web.reload(); true }
        }
        root.addView(toolbar)
        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        root.addView(progress, LinearLayout.LayoutParams(-1, (3 * resources.displayMetrics.density).toInt()))
        val error = TextView(this).apply {
            text = "Could not load this page. Check your connection and tap Refresh."
            setPadding(24, 24, 24, 24)
            visibility = View.GONE
        }
        root.addView(error)
        web = WebView(this)
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = true
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false)
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = navigate(request.url)
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = navigate(Uri.parse(url))
            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                error.visibility = View.GONE
                progress.visibility = View.VISIBLE
            }
            override fun onPageFinished(view: WebView, url: String?) {
                progress.visibility = View.GONE
                CookieManager.getInstance().flush()
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, failure: WebResourceError) {
                if (request.isForMainFrame) {
                    progress.visibility = View.GONE
                    error.visibility = View.VISIBLE
                }
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, value: Int) { progress.progress = value }
            override fun onShowFileChooser(view: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                chooser?.onReceiveValue(null)
                chooser = callback
                try {
                    pickFile.launch(params.createIntent())
                } catch (_: ActivityNotFoundException) {
                    chooser?.onReceiveValue(null)
                    chooser = null
                    Toast.makeText(this@WebToolsActivity, "No file picker is available.", Toast.LENGTH_LONG).show()
                }
                return true
            }
        }
        web.setDownloadListener { url, agent, disposition, mime, _ ->
            val uri = Uri.parse(url)
            if (uri.scheme != "https") {
                Toast.makeText(this, "This download format is not supported.", Toast.LENGTH_LONG).show()
                return@setDownloadListener
            }
            val name = URLUtil.guessFileName(url, disposition, mime).substringAfterLast('/')
            val request = DownloadManager.Request(uri)
                .setTitle(name)
                .setMimeType(mime)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "${System.currentTimeMillis()}-$name")
                .addRequestHeader("User-Agent", agent)
            CookieManager.getInstance().getCookie(url)?.let { request.addRequestHeader("Cookie", it) }
            try {
                (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                Toast.makeText(this, "Downloading $name", Toast.LENGTH_LONG).show()
            } catch (_: RuntimeException) {
                Toast.makeText(this, "Could not download this file. Please try again.", Toast.LENGTH_LONG).show()
            }
        }
        val sessionKey = java.security.MessageDigest.getInstance("SHA-256")
            .digest(SessionManager(this).getToken().orEmpty().toByteArray())
            .joinToString("") { "%02x".format(it) }
        val websitePrefs = getSharedPreferences("svd_website_tools", MODE_PRIVATE)
        val sameSession = websitePrefs.getString("owner", null) == sessionKey
        fun loadPage() {
            if (isFinishing || isDestroyed) return
            if (!sameSession || savedInstanceState == null || web.restoreState(savedInstanceState) == null) {
                web.loadUrl(initial.toString())
            }
        }
        if (sameSession) loadPage() else {
            // An app account change must never inherit another account's website cookies.
            CookieManager.getInstance().removeAllCookies {
                android.webkit.WebStorage.getInstance().deleteAllData()
                websitePrefs.edit().putString("owner", sessionKey).apply()
                loadPage()
            }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { goBack() }
        })
    }

    private fun navigate(uri: Uri): Boolean {
        if (uri.scheme == "https") return false
        // Calls, email and payment applications are deliberate non-website actions.
        if (uri.scheme in setOf("tel", "mailto", "upi", "phonepe", "gpay", "paytmmp")) {
            try { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            catch (_: ActivityNotFoundException) { Toast.makeText(this, "No app is available for this action.", Toast.LENGTH_LONG).show() }
        }
        return true
    }

    private fun goBack() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::web.isInitialized) web.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        chooser?.onReceiveValue(null)
        chooser = null
        if (::web.isInitialized) {
            (web.parent as? android.view.ViewGroup)?.removeView(web)
            web.stopLoading()
            web.destroy()
        }
        super.onDestroy()
    }

    companion object {
        fun intentForUrl(context: android.content.Context, url: String, title: String): Intent {
            val trimmed = url.trim()
            val secureUrl = when {
                trimmed.startsWith("https://", ignoreCase = true) -> trimmed
                trimmed.startsWith("http://", ignoreCase = true) -> "https://" + trimmed.substring(7)
                else -> "https://$trimmed"
            }
            return Intent(context, WebToolsActivity::class.java)
                .putExtra(EXTRA_URL, secureUrl).putExtra(EXTRA_TITLE, title)
        }
        private const val EXTRA_URL = "website_url"
        const val EXTRA_PATH = "website_tool_path"
        const val EXTRA_TITLE = "website_tool_title"
    }
}
