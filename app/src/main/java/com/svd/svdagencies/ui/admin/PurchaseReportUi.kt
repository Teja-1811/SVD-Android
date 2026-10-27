package com.svd.svdagencies.ui.admin

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.gson.JsonObject
import com.svd.svdagencies.R
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Compact native purchase report controls and summaries. */
internal class PurchaseReportUi(private val activity: AdminReportsActivity, private val content: LinearLayout) {
    private val ink = androidx.core.content.ContextCompat.getColor(activity, com.svd.svdagencies.R.color.textColorPrimary)
    private val muted = androidx.core.content.ContextCompat.getColor(activity, com.svd.svdagencies.R.color.textColorSecondary)
    val company = Spinner(activity)
    val firstDate: TextView
    val lastDate: TextView
    val refresh = MaterialButton(activity)
    val download = MaterialButton(activity)
    val status = text("Loading report…", 12f).apply { setTextColor(muted) }
    val results = com.svd.svdagencies.utils.CompactLinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }

    init {
        content.setPadding(dp(16), dp(12), dp(16), dp(20))
        val filters = column().apply { setPadding(dp(14), dp(14), dp(14), dp(14)) }
        content.addView(card(filters), block(12))
        filters.addView(text("Company", 12f).apply { setTextColor(muted) })
        val companyRow = row()
        company.background = ContextCompat.getDrawable(activity, R.drawable.bg_qty_outline)
        company.contentDescription = "Company"
        company.adapter = ArrayAdapter(activity, com.svd.svdagencies.R.layout.delivery_dropdown_item, listOf("All companies"))
        companyRow.addView(company, LinearLayout.LayoutParams(0, dp(50), 1f))
        refresh.text = ""
        refresh.contentDescription = "Refresh report"
        refresh.setIconResource(R.drawable.ic_refresh)
        refresh.iconTint = ColorStateList.valueOf(ink)
        refresh.iconPadding = 0
        refresh.iconSize = dp(22)
        refresh.minWidth = 0
        refresh.setPadding(dp(12), 0, dp(12), 0)
        refresh.backgroundTintList = ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(activity, com.svd.svdagencies.R.color.surface_background))
        refresh.strokeColor = ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(activity, com.svd.svdagencies.R.color.card_stroke_color))
        refresh.strokeWidth = dp(1)
        refresh.cornerRadius = dp(10)
        companyRow.addView(refresh, LinearLayout.LayoutParams(dp(50), dp(50)).apply { leftMargin = dp(8) })
        filters.addView(companyRow, block(10))

        val dates = row()
        val calendar = Calendar.getInstance()
        val year = activity.intent.getIntExtra("year", calendar.get(Calendar.YEAR))
        val month = activity.intent.getIntExtra("month", calendar.get(Calendar.MONTH) + 1)
        calendar.set(year, (month - 1).coerceIn(0, 11), 1)
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        firstDate = date(format.format(calendar.time), "From date")
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        lastDate = date(format.format(calendar.time), "To date")
        dates.addView(dateField("From date", firstDate), LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(5) })
        dates.addView(dateField("To date", lastDate), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(5) })
        filters.addView(dates)

        download.text = "Download PDF"
        download.isAllCaps = false
        download.textSize = 13f
        download.setIconResource(R.drawable.ic_download)
        download.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
        download.cornerRadius = dp(12)
        content.addView(download, LinearLayout.LayoutParams(-1, dp(52)).apply { bottomMargin = dp(6) })
        content.addView(status, block(8))
        content.addView(results)
    }

    fun render(data: JsonObject) {
        results.removeAllViews()
        val top = row()
        top.addView(stat("Purchases", money(data, "purchases")), weighted(true))
        top.addView(stat("Payments", money(data, "paid")), weighted(false))
        results.addView(top, block(10))
        val bottom = row()
        bottom.addView(stat("Period balance", money(data, "balance")), weighted(true))
        bottom.addView(stat("GST included", money(data, "gst")), weighted(false))
        results.addView(bottom, block(6))
        results.addView(text("Balance excludes opening dues. Negative means payments exceed purchases.", 12f).apply { visibility = android.view.View.GONE }.apply { setTextColor(muted) }, block(16))
        results.addView(text("Purchase history", 18f, true), block(10))
        val records = data.getAsJsonArray("records")
        if (records == null || records.size() == 0) {
            results.addView(card(text("No purchases for this selection.", 14f).apply { setPadding(dp(16), dp(20), dp(16), dp(20)) }))
            return
        }
        records.forEach { value ->
            val record = value.asJsonObject
            val body = column().apply { setPadding(dp(14), dp(14), dp(14), dp(14)) }
            val heading = row()
            heading.addView(text(displayDate(record.get("date")?.asString.orEmpty()), 14f, true), LinearLayout.LayoutParams(0, -2, 1f))
            heading.addView(text(money(record, "amount"), 17f, true))
            body.addView(heading, block(12))
            val taxes = row()
            listOf("CGST" to "cgst", "SGST" to "sgst", "IGST" to "igst").forEach { (label, key) ->
                val cell = column()
                cell.addView(text(label, 11f).apply { setTextColor(muted) })
                cell.addView(text(money(record, key), 13f))
                taxes.addView(cell, LinearLayout.LayoutParams(0, -2, 1f))
            }
            body.addView(taxes)
            results.addView(card(body), block(10))
        }
    }

    private fun stat(title: String, amount: String): MaterialCardView {
        val body = column().apply { setPadding(dp(14), dp(12), dp(14), dp(12)) }
        body.addView(text(title, 12f).apply { setTextColor(muted) })
        body.addView(text(amount, 18f, true).apply { setPadding(0, dp(6), 0, 0) })
        return card(body)
    }
    private fun date(value: String, label: String) = text(value, 13f).apply {
        gravity = Gravity.CENTER_VERTICAL
        contentDescription = label
        isSingleLine = true
        setPadding(dp(10), 0, dp(10), 0)
        background = ContextCompat.getDrawable(activity, R.drawable.bg_qty_outline)
        val icon = ContextCompat.getDrawable(activity, R.drawable.ic_calendar)?.mutate()
        icon?.setTint(ink); icon?.setBounds(0, 0, dp(18), dp(18))
        setCompoundDrawables(null, null, icon, null)
        compoundDrawablePadding = dp(4)
    }
    private fun dateField(title: String, control: TextView) = column().apply {
        addView(text(title, 12f).apply { setTextColor(muted) }, block(5))
        addView(control, LinearLayout.LayoutParams(-1, dp(48)))
    }
    private fun card(child: android.view.View) = MaterialCardView(activity).apply {
        radius = dp(14).toFloat(); cardElevation = 0f
        setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(activity, com.svd.svdagencies.R.color.surface_background)); strokeColor = androidx.core.content.ContextCompat.getColor(activity, com.svd.svdagencies.R.color.card_stroke_color); strokeWidth = dp(1)
        addView(child, ViewGroup.LayoutParams(-1, -2))
    }
    private fun row() = com.svd.svdagencies.utils.CompactLinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun column() = com.svd.svdagencies.utils.CompactLinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
    private fun text(value: String, size: Float, bold: Boolean = false) = TextView(activity).apply {
        text = value; textSize = size; setTextColor(ink)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun block(bottom: Int = 0) = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(bottom) }
    private fun weighted(first: Boolean) = LinearLayout.LayoutParams(0, -2, 1f).apply {
        if (first) rightMargin = dp(5) else leftMargin = dp(5)
    }
    private fun money(data: JsonObject, key: String) = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).format(data.get(key)?.takeUnless { it.isJsonNull }?.asDouble ?: 0.0)
    private fun displayDate(value: String): String = try {
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value)!!)
    } catch (_: Exception) { value }
    private fun dp(value: Int) = (value * activity.resources.displayMetrics.density).toInt()
}
