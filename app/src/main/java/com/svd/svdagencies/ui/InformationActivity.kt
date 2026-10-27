package com.svd.svdagencies.ui

import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.svd.svdagencies.R
import com.svd.svdagencies.base.BaseActivity
import com.svd.svdagencies.ui.customer.PortalNavigation
import com.svd.svdagencies.ui.customer.CustomerContactSupportActivity
import com.svd.svdagencies.utils.SessionManager
import com.svd.svdagencies.utils.UserRole
import org.json.JSONArray

/** Offline native copies of the published business information and policies. */
open class InformationActivity : BaseActivity() {
    protected open val defaultPage = "about"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pages = JSONArray(resources.openRawResource(R.raw.information_pages).bufferedReader().use { it.readText() })
        val key = intent.getStringExtra(EXTRA_PAGE) ?: defaultPage
        val page = (0 until pages.length()).map { pages.getJSONObject(it) }.firstOrNull { it.getString("id") == key } ?: pages.getJSONObject(0)
        val title = page.getString("title")
        val scroll = NestedScrollView(this).apply { id = R.id.informationScroll; isFillViewport = true; clipToPadding = false }
        val column = com.svd.svdagencies.utils.CompactLinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(28)) }
        scroll.addView(column, ViewGroup.LayoutParams(-1, -2))
        val account = SessionManager(this).getRole() in listOf(UserRole.CUSTOMER, UserRole.USER)
        if (account) {
            PortalNavigation.installPage(this, scroll, title, menuForPage(page.getString("id")))
        } else {
            val shell = com.svd.svdagencies.utils.CompactLinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            shell.addView(MaterialToolbar(this).apply {
                this.title = title
                setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
                navigationContentDescription = "Back"
                setNavigationOnClickListener { finish() }
            }, LinearLayout.LayoutParams(-1, dp(56)))
            shell.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
            setContentView(shell)
        }
        column.addView(text("SVD MILK AGENCIES", 12f, true).apply { setTextColor(primary()); letterSpacing = .08f })
        column.addView(text(title, 28f, true).apply { setPadding(0, dp(8), 0, dp(16)) })
        var section: LinearLayout? = null
        val blocks = page.getJSONArray("blocks")
        for (i in 0 until blocks.length()) {
            val block = blocks.getJSONObject(i)
            val heading = block.getString("type") == "heading"
            if (heading || section == null) {
                val card = MaterialCardView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) } }
                section = com.svd.svdagencies.utils.CompactLinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(16), dp(18), dp(16)) }
                card.addView(section)
                column.addView(card)
            }
            val value = (if (block.getString("type") == "bullet") "•  " else "") + block.getString("text")
            section!!.addView(text(value, if (heading) 20f else 16f, heading).apply { setPadding(0, dp(4), 0, dp(8)) })
        }
        if (page.getString("id") == "contact") {
            fun action(label: String, intent: Intent) {
                column.addView(MaterialButton(this).apply {
                    text = label
                    setOnClickListener {
                        try { startActivity(intent) } catch (_: ActivityNotFoundException) { Toast.makeText(this@InformationActivity, "No app is available for this action.", Toast.LENGTH_LONG).show() }
                    }
                }, LinearLayout.LayoutParams(-1, -2))
            }
            action("Call our team", Intent(Intent.ACTION_DIAL, Uri.parse("tel:+919392890375")))
            action("Email our team", Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:svdagencies12@gmail.com")))
            action("Find our location", Intent(Intent.ACTION_VIEW, Uri.parse("geo:16.7784167,81.2303611?q=16.7784167,81.2303611(SVD%20Milk%20Agency)")))
            if (account) action("Send a support enquiry", Intent(this, CustomerContactSupportActivity::class.java))
        }
    }
    private fun primary() = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, getColor(R.color.textColorPrimary))
    private fun text(value: String, size: Float, bold: Boolean) = TextView(this).apply {
        text = value; textSize = size; setTextColor(getColor(R.color.textColorPrimary))
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setLineSpacing(dp(3).toFloat(), 1f); setTextIsSelectable(true)
        layoutParams = LinearLayout.LayoutParams(-1, -2)
        if (bold) androidx.core.view.ViewCompat.setAccessibilityHeading(this, true)
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    companion object {
        private const val EXTRA_PAGE = "information_page"
        fun intent(context: Context, page: String) = Intent(context, InformationActivity::class.java).putExtra(EXTRA_PAGE, page)
        fun menuForPage(page: String) = when (page) {
            "contact" -> R.id.nav_contact_info; "privacy" -> R.id.nav_privacy; "terms" -> R.id.nav_terms
            "refund" -> R.id.nav_refund; "shipment" -> R.id.nav_shipment; else -> R.id.nav_about
        }
        fun pageForMenu(id: Int): String? = when (id) {
            R.id.nav_about -> "about"; R.id.nav_contact_info -> "contact"; R.id.nav_privacy -> "privacy"
            R.id.nav_terms -> "terms"; R.id.nav_refund -> "refund"; R.id.nav_shipment -> "shipment"; else -> null
        }
    }
}
