package com.svd.svdagencies.ui.admin

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.svd.svdagencies.data.model.admin.customerData.MonthlyStatementGroup
import java.util.Locale

object MonthlyStatementTable {
    fun create(context: Context, groups: List<MonthlyStatementGroup>): LinearLayout {
        fun dp(value: Int) = (context.resources.displayMetrics.density * value).toInt()
        fun money(value: Double) = "₹" + String.format(Locale.US, "%,.2f", value)
        fun cell(value: String, width: Int, merged: Boolean = false, header: Boolean = false): TextView = TextView(context).apply {
            text = value; textSize = if (header) 12f else 13f
            setTextColor(if (header) Color.WHITE else Color.rgb(24,59,71))
            if (header || merged) setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL or if (width == 200) Gravity.START else Gravity.CENTER
            setPadding(dp(8), dp(12), dp(8), dp(12)); minHeight = dp(52)
            background = GradientDrawable().apply { setColor(if(header) Color.rgb(24,59,71) else if(merged) Color.rgb(240,246,248) else Color.WHITE); setStroke(dp(1), Color.rgb(226,234,239)) }
            layoutParams = LinearLayout.LayoutParams(dp(width), if(merged) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val panel = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(0,dp(16),0,dp(16)) }
        panel.addView(TextView(context).apply { text="Daily statement · products by date"; textSize=18f; setTextColor(Color.rgb(24,59,71)); setTypeface(typeface,Typeface.BOLD); setPadding(0,0,0,dp(12)) })
        val scroll = HorizontalScrollView(context)
        val table = LinearLayout(context).apply { orientation=LinearLayout.VERTICAL }
        val widths = listOf(100,100,200,65,95,110,100,110,95,95)
        val header = LinearLayout(context)
        listOf("Date","Item code","Item name","Qty","Price","GST","Item total","Grand total","Paid","Due").forEachIndexed { index, label -> header.addView(cell(label,widths[index],header=true)) }
        table.addView(header)
        groups.forEach { group ->
            val row = LinearLayout(context).apply { orientation=LinearLayout.HORIZONTAL; layoutParams=LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT) }
            row.addView(cell(group.date,100,merged=true))
            val products = LinearLayout(context).apply { orientation=LinearLayout.VERTICAL }
            group.lines.orEmpty().forEach { line ->
                val product = LinearLayout(context)
                val gst = line.gst_rate?.let { "${String.format(Locale.US,"%.2f",it)}%\n${money(line.gst_amount ?: 0.0)}" } ?: "Not recorded"
                val values=listOf(line.item_code.orEmpty(),line.item_name.orEmpty(),String.format(Locale.US,"%.2f",line.quantity),money(line.rate),gst,money(line.amount))
                values.forEachIndexed { index,value -> product.addView(cell(value,widths[index+1])) }
                products.addView(product)
            }
            row.addView(products)
            row.addView(cell(money(group.total),110,merged=true));row.addView(cell(money(group.paid),95,merged=true));row.addView(cell(money(group.due),95,merged=true))
            table.addView(row)
        }
        scroll.addView(table);panel.addView(scroll)
        if(groups.isEmpty()) panel.addView(TextView(context).apply { text="No product records for this month.";setPadding(0,dp(12),0,0) })
        panel.addView(TextView(context).apply { text="Swipe sideways for all columns. Price is per unit before discounts; item totals include discounts and GST. Date totals are shown once.";textSize=12f;setPadding(0,dp(10),0,0) })
        return panel
    }
}
