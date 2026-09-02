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

    lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("svd_session", MODE_PRIVATE)
        
        // Enable Edge-to-Edge to correctly handle Keyboard Insets
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = getColor(com.svd.svdagencies.R.color.brand_red)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        // Ensure content is always above the keyboard
        findViewById<View>(R.id.content)?.let { rootView ->
            ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
                val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                
                // Add a small extra gap (8dp) for "space between keyboard and input" as requested
                val extraGap = (8 * resources.displayMetrics.density).toInt()
                val bottomPadding = if (imeInsets.bottom > 0) {
                    imeInsets.bottom + extraGap
                } else {
                    systemBars.bottom
                }
                
                // Set padding to respect both top (status bar) and bottom (keyboard/navigation)
                v.setPadding(0, systemBars.top, 0, bottomPadding)
                
                insets
            }
        }
    }

    fun logout() {
        prefs.edit().clear().apply()
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
