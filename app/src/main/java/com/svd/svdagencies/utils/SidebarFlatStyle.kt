package com.svd.svdagencies.utils

import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.svd.svdagencies.R
import java.util.WeakHashMap

/**
 * Applies the Admin/Delivery Agent drawer visual system to the flat retailer
 * and user menus. Their destinations remain flat by design; only their menu
 * hierarchy differs from the Admin tree.
 */
object SidebarFlatStyle {
    private val installed = WeakHashMap<NavigationView, Boolean>()

    fun install(nav: NavigationView) {
        if (installed.put(nav, true) == true) return
        fun dp(value: Int) = (value * nav.resources.displayMetrics.density + .5f).toInt()

        nav.itemMaxLines = 2
        nav.itemTextColor = ContextCompat.getColorStateList(nav.context, R.color.admin_sidebar_text)
        nav.itemIconTintList = ContextCompat.getColorStateList(nav.context, R.color.admin_sidebar_text)
        nav.setItemBackgroundResource(R.drawable.bg_admin_sidebar_item)
        nav.itemHorizontalPadding = dp(12)
        nav.itemVerticalPadding = dp(8)
        nav.layoutParams = nav.layoutParams.apply {
            width = minOf(dp(290), nav.resources.displayMetrics.widthPixels - dp(40))
        }
        (nav.getChildAt(0) as? RecyclerView)?.itemAnimator = null
        SidebarLogoutFooter.install(nav)

        val userPortal = SessionManager(nav.context).getRole() == UserRole.USER
        nav.getHeaderView(0).apply {
            findViewById<TextView>(R.id.txtAppName).text = PaymentConfig.PAYEE_NAME
            findViewById<TextView>(R.id.txtPortalName).text =
                if (userPortal) "User Portal" else "Retailer Portal"
        }
    }
}
