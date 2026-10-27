package com.svd.svdagencies.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.svd.svdagencies.R
import com.svd.svdagencies.base.BaseActivity
import com.svd.svdagencies.ui.auth.LoginActivity
import com.svd.svdagencies.utils.Appearance
import com.svd.svdagencies.utils.SessionManager
import com.svd.svdagencies.utils.UserRole

/** Settings are available to authenticated accounts; business details are admin only. */
class ToolsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = SessionManager(this)
        if (session.getToken().isNullOrBlank()) {
            startActivity(Intent(this, LoginActivity::class.java)); finish(); return
        }
        setContentView(R.layout.activity_tools)
        findViewById<MaterialToolbar>(R.id.toolsToolbar).apply {
            title = "Settings"
            setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            navigationContentDescription = getString(R.string.tools_back)
            setNavigationOnClickListener { finish() }
        }
        val ids = intArrayOf(R.id.paletteRuby, R.id.paletteOcean, R.id.paletteForest)
        findViewById<ChipGroup>(R.id.paletteChoices).apply {
            check(ids[Appearance.selected(this@ToolsActivity)])
            setOnCheckedStateChangeListener { _, checked ->
                val selected = ids.indexOf(checked.firstOrNull() ?: return@setOnCheckedStateChangeListener)
                if (selected >= 0 && selected != Appearance.selected(this@ToolsActivity)) {
                    Appearance.save(this@ToolsActivity, selected); recreate()
                }
            }
        }
        findViewById<LinearLayout>(R.id.toolsList).apply {
            removeAllViews()
            if (session.getRole() == UserRole.ADMIN) SettingsBusinessUi(this@ToolsActivity, this).attach()
            else visibility = View.GONE
        }
    }
}
