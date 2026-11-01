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
class ToolsActivity : com.svd.svdagencies.ui.admin.AdminBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = SessionManager(this)
        if (session.getToken().isNullOrBlank()) {
            startActivity(Intent(this, LoginActivity::class.java)); finish(); return
        }
        val section=intent.getStringExtra("settings_section") ?: "appearance"
        val layout=when(section) { "gst" -> R.layout.activity_settings_gst; "licences" -> R.layout.activity_settings_licences; else -> R.layout.activity_settings_appearance }
        val page=layoutInflater.inflate(layout,null)
        val oldToolbar=page.findViewById<MaterialToolbar>(R.id.toolsToolbar)
        (oldToolbar.parent as android.view.ViewGroup).removeView(oldToolbar)
        val title=when(section) { "gst" -> "GST Configuration"; "licences" -> "Licences"; else -> "Appearance" }
        if(session.getRole() in listOf(UserRole.CUSTOMER,UserRole.USER)) {
            com.svd.svdagencies.ui.customer.PortalNavigation.installPage(this,page,title,R.id.nav_tools_appearance)
        } else {
            setContentView(R.layout.admin_simple_dashboard)
            findViewById<LinearLayout>(R.id.contentContainer).addView(page)
            setupAdminLayout(title)
            findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh).isEnabled=false
            if(session.getRole()==UserRole.DELIVERY) {
                val nav=findViewById<com.google.android.material.navigation.NavigationView>(R.id.navigationView)
                nav.menu.clear();nav.inflateMenu(R.menu.menu_delivery_drawer)
                com.svd.svdagencies.ui.delivery.DeliveryNavigation.setup(this,findViewById(R.id.drawerLayout),nav,menuButton=findViewById(R.id.btnMenu),selectedItemId=R.id.nav_tools_appearance)
            }
        }
        val choices=findViewById<ChipGroup>(R.id.paletteChoices)
        val holder=choices.parent as android.view.ViewGroup
        val index=holder.indexOfChild(choices)
        val visible=choices.visibility==View.VISIBLE
        holder.removeView(choices)
        holder.addView(com.svd.svdagencies.utils.ThemePicker.choices(this).apply { visibility=if(visible)View.VISIBLE else View.GONE },index)
        findViewById<LinearLayout>(R.id.toolsList).apply {
            removeAllViews()
            if (session.getRole() == UserRole.ADMIN && section != "appearance") SettingsBusinessUi(this@ToolsActivity, this).attach()
            else visibility = View.GONE
        }

    }
}
