package com.svd.svdagencies.ui.delivery

import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.delivery.DeliveryAgentDuesResponse
import android.graphics.Typeface
import android.util.TypedValue
import android.widget.TextView

class DeliveryOverallReportActivity : DeliveryReportBaseActivity() {
    override val screenTitle: String = "Overall Report"
    override val selectedNavItem: Int = R.id.nav_delivery_overall_report
    override val sectionTitle: String = "Monthly Summary"

    override fun renderRows(response: DeliveryAgentDuesResponse): List<Pair<String, String>> {
        // This screen renders grouped report sections below instead of one long, mixed list.
        return emptyList()
    }

    override fun renderContent(response: DeliveryAgentDuesResponse) {
        listContainer.removeAllViews()

        addSummarySection(
            "Billing",
            listOf(
                "Bills generated" to response.summary.billCount.toString(),
                "Customer bills" to response.summary.customerBillCount.toString(),
                "Self bills" to response.summary.selfBillCount.toString(),
                "Customer invoices" to money(response.summary.customerBillAmount),
                "Self invoices" to money(response.summary.selfBillAmount),
            )
        )
        addSummarySection(
            "Collections",
            listOf(
                "Paid against bills" to money(response.summary.totalPaid),
                "Outstanding bills" to money(response.summary.totalDue),
                "Collected from customers" to money(response.summary.collectedAmount),
                "To submit at counter" to money(needToSubmitAmount(response)),
                "Submitted at counter" to money(submittedAmount(response)),
                "Remaining to submit" to money(remainingGeneratedAmount(response)),
            )
        )
        addSummarySection(
            "Earnings",
            listOf(
                "Total profit" to money(response.summary.totalProfit),
                "Salary earned" to money(salaryEarned(response)),
                "Salary paid" to money(response.summary.salaryPaid),
                "Pending salary" to money(pendingSalary(response)),
            )
        )

        addSectionHeader("Item-wise sales")
        if (response.items.isEmpty()) {
            addEmptyMessage("No item sales for this month.")
        } else {
            response.items.forEach { item ->
                val itemName = listOfNotNull(item.itemCode, item.itemName)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                    .ifBlank { "Item" }
                val row = layoutInflater.inflate(R.layout.delivery_report_item_sale, listContainer, false)
                row.findViewById<TextView>(R.id.tvItemName).text = itemName
                row.findViewById<TextView>(R.id.tvItemQuantity).text = formatQty(item.quantity)
                row.findViewById<TextView>(R.id.tvItemSales).text = money(item.totalAmount)
                listContainer.addView(row)
            }
        }
    }

    private fun addSummarySection(title: String, rows: List<Pair<String, String>>) {
        addSectionHeader(title)
        rows.forEach { (label, value) ->
            val row = layoutInflater.inflate(R.layout.admin_m_row_summary_item, listContainer, false)
            row.findViewById<TextView>(R.id.tvLabel).text = label
            row.findViewById<TextView>(R.id.tvValue).text = value
            listContainer.addView(row)
        }
    }

    private fun addSectionHeader(title: String) {
        val header = TextView(this).apply {
            text = title
            setTextColor(getColor(R.color.textColorPrimary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(16, 28, 16, 8)
        }
        listContainer.addView(header)
    }

    private fun addEmptyMessage(message: String) {
        val view = TextView(this).apply {
            text = message
            setTextColor(getColor(R.color.textColorSecondary))
            setPadding(16, 16, 16, 24)
        }
        listContainer.addView(view)
    }

    private fun formatQty(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)
    }
}
