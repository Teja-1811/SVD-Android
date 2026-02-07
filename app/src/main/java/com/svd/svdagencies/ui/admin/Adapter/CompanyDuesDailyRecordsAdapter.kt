package com.svd.svdagencies.ui.admin.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.admin.AdminSummaryItem

class CompanyDuesDailyRecordsAdapter(
    private var records: List<AdminSummaryItem>
) : RecyclerView.Adapter<CompanyDuesDailyRecordsAdapter.DailyRecordViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyRecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.admin_companies_due_daily_row, parent, false)
        return DailyRecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: DailyRecordViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount(): Int = records.size

    fun getRecords(): List<AdminSummaryItem> {
        return records
    }

    fun updateRecords(newRecords: List<AdminSummaryItem>) {
        records = newRecords
        notifyDataSetChanged()
    }

    inner class DailyRecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDay: TextView = itemView.findViewById(R.id.tvDay)
        private val etInvoice: EditText = itemView.findViewById(R.id.etInvoice)
        private val etPaid: EditText = itemView.findViewById(R.id.etPaid)

        private var invoiceWatcher: TextWatcher? = null
        private var paidWatcher: TextWatcher? = null

        fun bind(record: AdminSummaryItem) {
            tvDay.text = record.date

            etInvoice.clearFocus()
            etPaid.clearFocus()

            // Remove listeners before setting text to avoid triggering them
            etInvoice.removeTextChangedListener(invoiceWatcher)
            etPaid.removeTextChangedListener(paidWatcher)

            val invoiceText = if (record.invoice_amount != 0.0) String.format("%.2f", record.invoice_amount) else ""
            if (etInvoice.text.toString() != invoiceText) {
                etInvoice.setText(invoiceText)
            }

            val paidText = if (record.paid_amount != 0.0) String.format("%.2f", record.paid_amount) else ""
            if (etPaid.text.toString() != paidText) {
                etPaid.setText(paidText)
            }

            invoiceWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        records[adapterPosition].invoice_amount = s.toString().toDoubleOrNull() ?: 0.0
                    }
                }
            }

            paidWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        records[adapterPosition].paid_amount = s.toString().toDoubleOrNull() ?: 0.0
                    }
                }
            }

            etInvoice.addTextChangedListener(invoiceWatcher)
            etPaid.addTextChangedListener(paidWatcher)
        }
    }
}
