package com.svd.svdagencies.ui.admin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.admin.AdminSummaryItem
import com.svd.svdagencies.data.model.admin.CompanyPayment
import com.svd.svdagencies.data.model.admin.PaymentData
import java.util.Locale

class CompanyPaymentsAdapter(
    private var companies: List<CompanyPayment>
) : RecyclerView.Adapter<CompanyPaymentsAdapter.CompanyViewHolder>() {

    private val recordsAdapters = mutableMapOf<Int, CompanyDuesDailyRecordsAdapter>()

    fun updateList(newCompanies: List<CompanyPayment>) {
        companies = newCompanies
        recordsAdapters.clear()
        notifyDataSetChanged()
    }

    fun getPaymentDataWithContext(year: Int, month: Int): Map<String, Map<String, PaymentData>> {
        val result = mutableMapOf<String, Map<String, PaymentData>>()
        
        companies.forEach { company ->
            val adapter = recordsAdapters[company.company_id]
            
            val latestRecords = if (adapter != null) {
                adapter.getRecords() 
            } else {
                 convertDailyRecords(company.records)
            }

            val dailyMap = mutableMapOf<String, PaymentData>()
            latestRecords.forEach { item ->
                val day = item.date
                val monthStr = if (month < 10) "0$month" else "$month"
                val fullDate = "$year-$monthStr-$day"
                
                dailyMap[fullDate] = PaymentData(
                    invoice = if (item.invoice_amount == 0.0) null else item.invoice_amount,
                    paid = if (item.paid_amount == 0.0) null else item.paid_amount
                )
            }
            result[company.company_id.toString()] = dailyMap
        }
        return result
    }
    
    private fun convertDailyRecords(records: List<com.svd.svdagencies.data.model.admin.DailyRecord>): List<AdminSummaryItem> {
         return records.map { record ->
            val day = try {
                record.date.substring(8, 10)
            } catch (e: Exception) {
                record.date
            }
            AdminSummaryItem(
                date = day, 
                invoice_amount = record.invoice_amount, 
                paid_amount = record.paid_amount
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompanyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.admin_companies_due_card, parent, false)
        return CompanyViewHolder(view)
    }

    override fun onBindViewHolder(holder: CompanyViewHolder, position: Int) {
        holder.bind(companies[position])
    }

    override fun getItemCount(): Int = companies.size

    inner class CompanyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCompanyName: TextView = itemView.findViewById(R.id.tvCompanyName)
        private val tvHeaderInvoice: TextView = itemView.findViewById(R.id.tvHeaderInvoice)
        private val tvHeaderPaid: TextView = itemView.findViewById(R.id.tvHeaderPaid)
        private val tvHeaderDue: TextView = itemView.findViewById(R.id.tvHeaderDue)
        private val tvHeaderAdvance: TextView = itemView.findViewById(R.id.tvHeaderAdvance)
        private val rvDailyRecords: RecyclerView = itemView.findViewById(R.id.rvDailyRecords)

        init {
            rvDailyRecords.layoutManager = LinearLayoutManager(itemView.context)
            rvDailyRecords.setHasFixedSize(true)
            rvDailyRecords.isNestedScrollingEnabled = false
        }

        fun bind(company: CompanyPayment) {
            tvCompanyName.text = company.company_name
            
            updateCompanySummary(company)

            var adapter = recordsAdapters[company.company_id]
            if (adapter == null) {
                val summaryItems = convertDailyRecords(company.records)
                adapter = CompanyDuesDailyRecordsAdapter(summaryItems) {
                    // Update totals when records change
                    recalculateCompanyTotals(company, summaryItems)
                }
                recordsAdapters[company.company_id] = adapter
            }
            
            if (rvDailyRecords.adapter !== adapter) {
                rvDailyRecords.adapter = adapter
            }
        }

        private fun recalculateCompanyTotals(company: CompanyPayment, items: List<AdminSummaryItem>) {
            val totalInvoice = items.sumOf { it.invoice_amount }
            val totalPaid = items.sumOf { it.paid_amount }
            
            // This is a bit tricky because CompanyPayment is usually immutable (val)
            // But we can update the UI directly for real-time feedback
            updateSummaryUI(totalInvoice, totalPaid)
        }

        private fun updateCompanySummary(company: CompanyPayment) {
            updateSummaryUI(company.total_invoice, company.total_paid)
        }

        private fun updateSummaryUI(totalInvoice: Double, totalPaid: Double) {
            val diff = totalInvoice - totalPaid
            val due = if (diff > 0) diff else 0.0
            val advance = if (diff < 0) -diff else 0.0

            tvHeaderInvoice.text = String.format(Locale.US, "₹%.2f", totalInvoice)
            tvHeaderPaid.text = String.format(Locale.US, "₹%.2f", totalPaid)
            tvHeaderDue.text = String.format(Locale.US, "₹%.2f", due)
            tvHeaderAdvance.text = String.format(Locale.US, "₹%.2f", advance)
        }
    }
}
