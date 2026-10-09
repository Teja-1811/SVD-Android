package com.svd.svdagencies.ui.delivery

import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.delivery.DeliveryAgentDuesResponse

class DeliveryOverallReportActivity : DeliveryReportBaseActivity() {
    override val screenTitle: String = "Overall Report"
    override val selectedNavItem: Int = R.id.nav_delivery_overall_report
    override val sectionTitle: String = "Sale Details"

    override fun renderRows(response: DeliveryAgentDuesResponse): List<Pair<String, String>> {
        val summaryRows = listOf(
            Pair("Generated bills", response.summary.billCount.toString()),
            Pair("Bills to customers count", response.summary.customerBillCount.toString()),
            Pair("Self bills count", response.summary.selfBillCount.toString()),
            Pair("Customers invoice total", money(response.summary.customerBillAmount)),
            Pair("Self bills invoice total", money(response.summary.selfBillAmount)),
            Pair("Total paid against bills", money(response.summary.totalPaid)),
            Pair("Outstanding bill amount", money(response.summary.totalDue)),
            Pair("Collected from customers", money(response.summary.collectedAmount)),
            Pair("Amount to submit to counter", money(needToSubmitAmount(response))),
            Pair("Total submitted amount", money(submittedAmount(response))),
            Pair("Remaining to submit", money(remainingGeneratedAmount(response))),
            Pair("Total profit", money(response.summary.totalProfit)),
            Pair("Salary earned", money(salaryEarned(response))),
            Pair("Salary paid", money(response.summary.salaryPaid)),
            Pair("Pending salary", money(pendingSalary(response)))
        )

        val itemHeader = listOf(Pair("--- Item-wise Sales ---", ""))
        
        val itemRows = response.items.map {
            val itemName = listOfNotNull(it.itemCode, it.itemName)
                .filter { value -> value.isNotBlank() }
                .joinToString(" - ")
            Pair(itemName.ifBlank { "Item" }, "Qty ${formatQty(it.quantity)}  |  ${money(it.totalAmount)}")
        }

        return summaryRows + itemHeader + itemRows
    }

    private fun formatQty(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)
    }
}
