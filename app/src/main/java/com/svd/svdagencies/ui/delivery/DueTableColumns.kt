package com.svd.svdagencies.ui.delivery

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.TextView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.delivery.DeliveryAgentDailyDueRecord
import java.util.Locale
import kotlin.math.ceil

/** One content-based column grid shared by the header and every recycled row. */
class DueTableColumns(private val context: Context) {
    private val ids = intArrayOf(R.id.tvDueDate, R.id.tvDueAgent, R.id.tvDueOpening,
        R.id.tvDueCollected, R.id.tvDueTotal, R.id.tvDueSubmitted, R.id.tvDueBalance)
    private val headings = listOf("Date", "Agent Name", "Op Due", "Collected", "Total", "Submitted", "Due")
    private var widths = IntArray(ids.size)
    init { update(emptyList()) }

    fun update(rows: List<DeliveryAgentDailyDueRecord>) {
        val probe = TextView(context).apply { textSize = 12f }
        val padding = ceil(24 * context.resources.displayMetrics.density).toInt()
        widths = IntArray(ids.size) { column ->
            probe.typeface = Typeface.DEFAULT_BOLD
            var width = probe.paint.measureText(headings[column])
            rows.forEach { row ->
                probe.typeface = if (column == 6) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                val text = when (column) {
                    0 -> row.date.orEmpty()
                    1 -> row.agentName ?: row.deliveryAgentId?.let { "Agent #$it" } ?: "—"
                    else -> String.format(Locale.US, "%.2f", when (column) {
                        2 -> row.opDue; 3 -> row.amountCollected; 4 -> row.total
                        5 -> row.settledAmount; else -> row.due
                    })
                }
                width = maxOf(width, probe.paint.measureText(text))
            }
            ceil(width).toInt() + padding
        }
    }

    fun apply(root: View) {
        (root as? HorizontalScrollView)?.isFillViewport = false
        val row = (root as? ViewGroup)?.getChildAt(0) as? ViewGroup ?: return
        row.minimumWidth = 0
        row.layoutParams = row.layoutParams.apply { width = widths.sum() }
        ids.forEachIndexed { index, id ->
            root.findViewById<TextView>(id).apply {
                gravity = Gravity.CENTER
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                layoutParams = layoutParams.apply { width = widths[index] }
            }
        }
    }
}
