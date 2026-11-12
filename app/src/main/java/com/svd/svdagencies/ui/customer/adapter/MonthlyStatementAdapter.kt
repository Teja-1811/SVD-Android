package com.svd.svdagencies.ui.customer.adapter

import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.customer.MonthlyStatementGroup
import java.text.SimpleDateFormat
import java.util.Locale

class MonthlyStatementAdapter : RecyclerView.Adapter<MonthlyStatementAdapter.Holder>() {
    private var items: List<MonthlyStatementGroup> = emptyList()
    fun submitList(value: List<MonthlyStatementGroup>) { items = value; notifyDataSetChanged() }
    override fun getItemCount() = items.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(parent)
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])

    class Holder(parent: ViewGroup) : RecyclerView.ViewHolder(card(parent)) {
        private val root = itemView.findViewWithTag<LinearLayout>("content")
        fun bind(group: MonthlyStatementGroup) {
            root.removeAllViews()
            root.addView(text(root, group.invoiceNumber, 16f, true))
            root.addView(text(root, displayDate(group.date) + "  •  ${group.items.size} item(s)", 12f, false))
            addDivider(root, 12)
            root.addView(text(root, "Item details", 13f, true).apply { setPadding(0, 0, 0, dp(root, 5)) })
            root.addView(itemRow(root, listOf("Item", "Qty", "Rate", "Disc", "Total"), true))
            group.items.forEach { item ->
                root.addView(itemRow(root, listOf(
                    item.itemCode,
                    clean(item.quantity),
                    money(item.rate),
                    money(item.discount),
                    money(item.amount),
                ), false))
            }
            addDivider(root, 10)
            root.addView(text(root, "Bill summary", 13f, true).apply { setPadding(0, 0, 0, dp(root, 4)) })
            root.addView(summaryRow(root, "Billed", money(group.total), "Paid", money(group.paid)))
            root.addView(text(root, "Opening due ${money(group.openingDue)}", 12f, false).apply { setPadding(0, dp(root, 7), 0, 0) })
            root.addView(text(root, "Balance due ${money(group.due)}", 14f, true).apply {
                setTextColor(ContextCompat.getColor(root.context, R.color.textColorPrimary)); gravity = Gravity.END; setPadding(0, dp(root, 6), 0, 0)
            })
        }
        companion object {
            private fun card(parent: ViewGroup): MaterialCardView = MaterialCardView(parent.context).apply {
                val margin = (10 * parent.resources.displayMetrics.density).toInt()
                layoutParams = RecyclerView.LayoutParams(-1, -2).apply { bottomMargin = margin }
                radius = dp(this, 16).toFloat(); cardElevation = 0f; strokeWidth = dp(this, 1)
                strokeColor = ContextCompat.getColor(context, R.color.card_stroke_color)
                addView(LinearLayout(context).apply { tag = "content"; orientation = LinearLayout.VERTICAL; setPadding(dp(this, 16), dp(this, 14), dp(this, 16), dp(this, 14)) })
            }
            private fun text(root: LinearLayout, value: String, size: Float, bold: Boolean) = TextView(root.context).apply {
                text = value; textSize = size; setTextColor(ContextCompat.getColor(context, R.color.textColorSecondary))
                if (bold) { setTypeface(typeface, Typeface.BOLD); setTextColor(ContextCompat.getColor(context, R.color.textColorPrimary)) }
            }
            private fun itemRow(root: LinearLayout, values: List<String>, heading: Boolean) = LinearLayout(root.context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, if (heading) 0 else dp(root, 7), 0, 0)
                val weights = floatArrayOf(1.35f, .48f, .95f, .9f, 1.12f)
                values.forEachIndexed { index, value ->
                    addView(text(root, value, if (heading) 11f else 12f, heading).apply {
                        gravity = if (index == 0) Gravity.START else Gravity.END
                        if (heading) setTextColor(ContextCompat.getColor(root.context, R.color.textColorSecondary))
                    }, LinearLayout.LayoutParams(0, -2, weights[index]))
                }
            }
            private fun addDivider(root: LinearLayout, topMarginDp: Int) {
                root.addView(View(root.context).apply { setBackgroundColor(ContextCompat.getColor(root.context, R.color.divider_line)) },
                    LinearLayout.LayoutParams(-1, dp(root, 1)).apply { topMargin = dp(root, topMarginDp); bottomMargin = dp(root, 3) })
            }
            private fun summaryRow(root: LinearLayout, leftLabel: String, leftValue: String, rightLabel: String, rightValue: String) = LinearLayout(root.context).apply {
                orientation = LinearLayout.HORIZONTAL
                listOf("$leftLabel  $leftValue", "$rightLabel  $rightValue").forEach { value ->
                    addView(text(root, value, 12f, false), LinearLayout.LayoutParams(0, -2, 1f))
                }
            }
            private fun money(value: Double) = "₹%.2f".format(Locale.US, value)
            private fun clean(value: Double) = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
            private fun displayDate(value: String) = runCatching { SimpleDateFormat("dd MMM yyyy", Locale.US).format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value)!!) }.getOrDefault(value)
            private fun dp(view: android.view.View, value: Int) = (value * view.resources.displayMetrics.density).toInt()
        }
    }
}
