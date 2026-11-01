package com.svd.svdagencies.utils

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.svd.svdagencies.R

/** Keep sign out reachable while navigation destinations scroll independently. */
object SidebarLogoutFooter {
    fun install(nav: NavigationView) {
        val logout = nav.menu.findItem(R.id.nav_logout) ?: return
        logout.isVisible = false
        if (nav.getTag(R.id.nav_logout) is View) return
        fun dp(value: Int) = (value * nav.resources.displayMetrics.density + .5f).toInt()
        val footerHeight = dp(44)
        // NavigationView's presenter is its scrolling first child.
        nav.getChildAt(0)?.let { list ->
            val params = list.layoutParams as? FrameLayout.LayoutParams
            if (params != null) {
                params.bottomMargin = footerHeight + dp(24)
                list.layoutParams = params
            }
        }
        val button = MaterialButton(nav.context).apply {
            text = "Log out"
            textSize = 14f
            iconSize = dp(18)
            isAllCaps = false
            icon = androidx.appcompat.content.res.AppCompatResources.getDrawable(context, R.drawable.ic_logout)
            iconPadding = dp(8)
            cornerRadius = dp(12)
            insetTop = 0
            insetBottom = 0
            setOnClickListener { nav.menu.performIdentifierAction(R.id.nav_logout, 0) }
        }
        nav.setTag(R.id.nav_logout, button)
        nav.addView(button, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, footerHeight, Gravity.BOTTOM).apply {
            setMargins(dp(12), 0, dp(12), dp(12))
        })
        ActionButtonColors.apply(button)
    }
}
