package com.svd.svdagencies.base


import android.R
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.svd.svdagencies.ui.auth.LoginActivity
import com.svd.svdagencies.utils.LoadingOverlayManager

open class BaseActivity : AppCompatActivity() {

    private var appliedPalette = -1
    private var compactRoot: View? = null
    private val compactLayoutListener = android.view.ViewTreeObserver.OnGlobalLayoutListener {
        compactRoot?.let { com.svd.svdagencies.utils.CompactUi.applyTree(it); com.svd.svdagencies.utils.CollapsibleNavigation.applyTree(this, it) }
    }

    override fun onContentChanged() {
        super.onContentChanged()
        compactRoot?.viewTreeObserver?.takeIf { it.isAlive }?.removeOnGlobalLayoutListener(compactLayoutListener)
        compactRoot = findViewById(android.R.id.content)
        compactRoot?.let { root ->
            root.viewTreeObserver.addOnGlobalLayoutListener(compactLayoutListener)
            root.post { if (!isDestroyed) { com.svd.svdagencies.utils.CompactUi.applyTree(root); com.svd.svdagencies.utils.CollapsibleNavigation.applyTree(this, root) } }
        }
    }

    override fun onDestroy() {
        compactRoot?.viewTreeObserver?.takeIf { it.isAlive }?.removeOnGlobalLayoutListener(compactLayoutListener)
        compactRoot = null
        super.onDestroy()
    }

    lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        appliedPalette = com.svd.svdagencies.utils.Appearance.selected(this)
        setTheme(com.svd.svdagencies.utils.Appearance.theme(this))
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("svd_session", MODE_PRIVATE)
        
        // Enable Edge-to-Edge to correctly handle Keyboard Insets
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = if ((resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES) androidx.core.content.ContextCompat.getColor(this, com.svd.svdagencies.R.color.status_bar) else com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, android.graphics.Color.BLACK)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        // Ensure content is always above the keyboard
        findViewById<View>(R.id.content)?.let { rootView ->
            ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
                val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
                
                // Add a small extra gap (8dp) for "space between keyboard and input" as requested
                val extraGap = (8 * resources.displayMetrics.density).toInt()
                val bottomPadding = if (imeInsets.bottom > 0) {
                    imeInsets.bottom + extraGap
                } else {
                    systemBars.bottom
                }
                
                // Set padding to respect both top (status bar) and bottom (keyboard/navigation)
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding)
                
                insets
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (appliedPalette != com.svd.svdagencies.utils.Appearance.selected(this)) {
            recreate()
            return
        }
        val session = com.svd.svdagencies.utils.SessionManager(this)
        if (!session.getToken().isNullOrBlank() && session.consumeNotificationPermissionPrompt()) {
            com.svd.svdagencies.notifications.PushRegistrationManager.requestPermissionIfNeeded(this)
        }
    }

    fun logout() {
        com.svd.svdagencies.utils.SessionManager(this).logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    protected fun showScreenLoading() {
        LoadingOverlayManager.show(this)
    }

    protected fun hideScreenLoading() {
        LoadingOverlayManager.hide(this)
    }

    fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus()
        }
    }
}
