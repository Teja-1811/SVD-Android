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
    override fun onContentChanged() {
        super.onContentChanged()
        findViewById<View>(android.R.id.content)?.let { root ->
            root.post {
                if (!isDestroyed) {
                    applyHeaderTheme(root)
                    com.svd.svdagencies.utils.CompactUi.applyTree(root)
                    com.svd.svdagencies.utils.CollapsibleNavigation.applyTree(this, root)
                }
            }
        }
    }

    private fun applyHeaderTheme(root: View) {
        val primary = com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, android.graphics.Color.BLACK)
        val onPrimary = com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimary, android.graphics.Color.WHITE)
        fun tintContent(view: View) {
            fun dateChip(node: View): Boolean {
                var candidate: View? = node
                while (candidate != null && candidate !is androidx.appcompat.widget.Toolbar) {
                    val name = runCatching { candidate!!.resources.getResourceEntryName(candidate!!.id) }.getOrDefault("")
                    if (name.contains("date", true) || name.contains("month", true)) return true
                    candidate = candidate.parent as? View
                }
                return false
            }
            if (view.tag == "svd-header-outline") return
            val foreground = if (dateChip(view)) primary else onPrimary
            when (view) {
                is android.widget.TextView -> view.setTextColor(foreground)
                is android.widget.ImageView -> view.imageTintList = android.content.res.ColorStateList.valueOf(foreground)
            }
            if (view is android.view.ViewGroup) for (index in 0 until view.childCount) tintContent(view.getChildAt(index))
        }
        fun visit(view: View) {
            if (view is com.google.android.material.appbar.AppBarLayout) {
                view.setBackgroundColor(primary)
                view.backgroundTintList = android.content.res.ColorStateList.valueOf(primary)
            }
            if (view is androidx.appcompat.widget.Toolbar) {
                view.setBackgroundColor(primary)
                view.backgroundTintList = android.content.res.ColorStateList.valueOf(primary)
                view.setTitleTextColor(onPrimary)
                view.setSubtitleTextColor(onPrimary)
                view.navigationIcon = view.navigationIcon?.mutate()?.apply { setTint(onPrimary) }
                view.overflowIcon = view.overflowIcon?.mutate()?.apply { setTint(onPrimary) }
                tintContent(view)
            }
            if (view is android.view.ViewGroup) for (index in 0 until view.childCount) visit(view.getChildAt(index))
        }
        visit(root)
    }

    lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
        appliedPalette = com.svd.svdagencies.utils.Appearance.selected(this)
        setTheme(com.svd.svdagencies.utils.Appearance.theme(this))
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("svd_session", MODE_PRIVATE)
        supportFragmentManager.registerFragmentLifecycleCallbacks(object : androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentViewCreated(fm: androidx.fragment.app.FragmentManager, fragment: androidx.fragment.app.Fragment, view: View, state: Bundle?) {
                view.post {
                    if (!isDestroyed) findViewById<View>(android.R.id.content)?.let {
                        com.svd.svdagencies.utils.CollapsibleNavigation.applyTree(this@BaseActivity, it)
                    }
                }
            }
        }, true)
        
        // Enable Edge-to-Edge to correctly handle Keyboard Insets
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, android.graphics.Color.BLACK)
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
