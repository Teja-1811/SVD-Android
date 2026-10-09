package com.svd.svdagencies.ui.delivery

import android.view.View
import android.widget.TextView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.delivery.DeliveryAgentDuesResponse
import com.svd.svdagencies.databinding.CustomerPaymentRowBinding
import java.text.SimpleDateFormat
import java.util.Locale

class DeliveryPaymentsActivity : DeliveryReportBaseActivity() {
    override val screenTitle: String = "Payments"
    override val selectedNavItem: Int = R.id.nav_delivery_payments
    override val sectionTitle: String = "Customer Payments"

    private val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    private val outputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)

    override fun renderContent(response: DeliveryAgentDuesResponse) {
        val listContainer = findViewById<android.widget.LinearLayout>(R.id.listContainer)
        listContainer.removeAllViews()

        // Render Header Rows (Summary)
        val headerRows = listOf(
            Pair("Total bills generated", response.summary.billCount.toString()),
            Pair("Total invoice amount", money(response.summary.totalAmount)),
            Pair("Total paid amount", money(response.summary.totalPaid)),
            Pair("Agent current due", money(currentAgentDue(response))),
            Pair("Salary earned", money(salaryEarned(response))),
            Pair("Salary paid", money(response.summary.salaryPaid)),
            Pair("Pending salary", money(pendingSalary(response))),
            Pair("Bills to customers invoice", money(response.summary.customerBillAmount)),
            Pair("Self bills invoice", money(response.summary.selfBillAmount)),
            Pair("Collected from customers", money(response.summary.collectedAmount)),
            Pair("Amount to submit", money(needToSubmitAmount(response))),
            Pair("Submitted amount", money(submittedAmount(response))),
            Pair("Remaining to submit", money(remainingGeneratedAmount(response)))
        )

        for (row in headerRows) {
            val view = layoutInflater.inflate(R.layout.admin_m_row_summary_item, listContainer, false)
            view.findViewById<TextView>(R.id.tvLabel).text = row.first
            view.findViewById<TextView>(R.id.tvValue).text = row.second
            listContainer.addView(view)
        }

        // Add a divider or space before cards
        val spacer = View(this)
        spacer.layoutParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            resources.getDimensionPixelSize(R.dimen.spacing_medium)
        )
        listContainer.addView(spacer)

        // Daily due summaries and customer payment events are different
        // records.  Show both instead of replacing payment history with the
        // daily aggregate.
        val dailyDues = response.dailyDues
        if (dailyDues.isNotEmpty()) {
            addSectionTitle(listContainer, "Daily collection settlements")
            dailyDues.forEach { due ->
                addSummaryRow(
                    listContainer,
                    due.date,
                    "Collected ${money(due.amountCollected)}  |  Submitted ${money(due.settledAmount)}  |  Balance ${money(due.balanceDue)}"
                )
            }
        }

        val result = response.results.firstOrNull()
        val payments = response.customerPaymentRecords.ifEmpty { result?.paymentRecords.orEmpty() }

        if (payments.isNotEmpty()) {
            addSectionTitle(listContainer, "Customer payment history")
        }

        if (payments.isEmpty()) {
            if (dailyDues.isEmpty()) {
                val tv = TextView(this)
                tv.text = "No payment records found for this period."
                tv.setPadding(32, 48, 32, 48)
                tv.gravity = android.view.Gravity.CENTER
                tv.setTextColor(getColor(R.color.textColorSecondary))
                listContainer.addView(tv)
            }
            return
        }

        for (item in payments) {
            val binding = CustomerPaymentRowBinding.inflate(layoutInflater, listContainer, false)
            
            val customer = item.customerName ?: "Customer"
            val invoice = item.invoiceNumber ?: "No Invoice"
            binding.tvPaymentFor.text = "$customer\n$invoice"
            
            binding.tvAmount.text = money(item.amount)
            binding.tvPaymentMethod.text = "Method: ${item.method ?: "N/A"}"
            binding.tvTransactionId.text = "TXN: ${item.transactionId ?: "N/A"}"
            binding.tvStatus.text = item.status?.uppercase() ?: "UNKNOWN"

            val statusBg = when (item.status?.lowercase()) {
                "success", "paid", "completed" -> R.drawable.bg_status_green
                "pending", "processing" -> R.drawable.bg_status_yellow
                "failed", "cancelled" -> R.drawable.bg_status_red
                else -> R.drawable.bg_status_yellow
            }
            binding.tvStatus.setBackgroundResource(statusBg)

            try {
                item.createdAt?.let {
                    val date = inputFormat.parse(it)
                    binding.tvPaymentDate.text = outputFormat.format(date)
                }
            } catch (e: Exception) {
                binding.tvPaymentDate.text = item.createdAt ?: "N/A"
            }

            listContainer.addView(binding.root)
        }
    }

    // This is still required by the base class, but we don't use it anymore as we override renderContent
    override fun renderRows(response: DeliveryAgentDuesResponse): List<Pair<String, String>> = emptyList()

    private fun addSectionTitle(container: android.widget.LinearLayout, title: String) {
        val titleView = TextView(this)
        titleView.text = title
        titleView.setTextColor(getColor(R.color.textColorPrimary))
        titleView.setPadding(16, 24, 16, 12)
        titleView.textSize = 16f
        titleView.setTypeface(null, android.graphics.Typeface.BOLD)
        container.addView(titleView)
    }

    private fun addSummaryRow(container: android.widget.LinearLayout, label: String, value: String) {
        val view = layoutInflater.inflate(R.layout.admin_m_row_summary_item, container, false)
        view.findViewById<TextView>(R.id.tvLabel).text = label
        view.findViewById<TextView>(R.id.tvValue).text = value
        container.addView(view)
    }
}
