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

class PaymentDailyRecordsAdapter(
    private var records: List<AdminSummaryItem>
) : RecyclerView.Adapter<PaymentDailyRecordsAdapter.DailyRecordViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyRecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.admin_companies_payment_daily_row, parent, false)
        return DailyRecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: DailyRecordViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount(): Int = records.size
    
    fun getRecords(): List<AdminSummaryItem> {
        return records
    }

    inner class DailyRecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDay: TextView = itemView.findViewById(R.id.tvDay)
        private val etInvoice: EditText = itemView.findViewById(R.id.etInvoice)
        private val etPaid: EditText = itemView.findViewById(R.id.etPaid)

        fun bind(record: AdminSummaryItem) {
            tvDay.text = record.date
            
            // Clear previous listeners to avoid feedback loops
            etInvoice.setOnFocusChangeListener(null)
            etPaid.setOnFocusChangeListener(null)

            if (record.invoice_amount != 0.0) {
                 etInvoice.setText(String.format("%.2f", record.invoice_amount))
            } else {
                 etInvoice.setText("")
            }

            if (record.paid_amount != 0.0) {
                 etPaid.setText(String.format("%.2f", record.paid_amount))
            } else {
                 etPaid.setText("")
            }

            // Simple text watchers to update model
            etInvoice.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val amount = s.toString().toDoubleOrNull() ?: 0.0
                    record.invoice_amount = amount
                }
            })

            etPaid.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val amount = s.toString().toDoubleOrNull() ?: 0.0
                    record.paid_amount = amount
                }
            })
        }
    }
}