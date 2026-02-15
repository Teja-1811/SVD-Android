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

class CompanyPaymentsAdapter(
    private var companies: List<CompanyPayment>
) : RecyclerView.Adapter<CompanyPaymentsAdapter.CompanyViewHolder>() {

    private val recordsAdapters = mutableMapOf<Int, CompanyDuesDailyRecordsAdapter>()

    fun updateList(newCompanies: List<CompanyPayment>) {
        companies = newCompanies
        // We don't clear recordsAdapters here to maintain the state of input fields
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
        private val tvCompanyTotals: TextView = itemView.findViewById(R.id.tvCompanyTotals)
        private val rvDailyRecords: RecyclerView = itemView.findViewById(R.id.rvDailyRecords)

        init {
            rvDailyRecords.layoutManager = LinearLayoutManager(itemView.context)
            // Crucial: Optimization to prevent focus issues in nested RecyclerViews
            rvDailyRecords.setHasFixedSize(true)
            rvDailyRecords.isNestedScrollingEnabled = false
        }

        fun bind(company: CompanyPayment) {
            tvCompanyName.text = company.company_name
            tvCompanyTotals.text = "Invoice: ₹${company.total_invoice} | Paid: ₹${company.total_paid} | Due: ₹${company.remaining_due}"

            var adapter = recordsAdapters[company.company_id]
            if (adapter == null) {
                val summaryItems = convertDailyRecords(company.records)
                adapter = CompanyDuesDailyRecordsAdapter(summaryItems)
                recordsAdapters[company.company_id] = adapter
            }
            
            // Re-assign the adapter only if it's different to avoid resetting scroll position/focus
            if (rvDailyRecords.adapter !== adapter) {
                rvDailyRecords.adapter = adapter
            }
        }
    }
}