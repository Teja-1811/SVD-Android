package com.svd.svdagencies.ui.admin.adapter

import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.admin.AdminSummaryItem
import java.util.Locale

class CompanyDuesDailyRecordsAdapter(
    private var records: List<AdminSummaryItem>,
    private val onAmountChanged: () -> Unit
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

    inner class DailyRecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDay: TextView = itemView.findViewById(R.id.tvDay)
        private val tvInvoice: TextView = itemView.findViewById(R.id.tvInvoice)
        private val etPaid: EditText = itemView.findViewById(R.id.etPaid)
        private val tvDueCredit: TextView = itemView.findViewById(R.id.tvDueCredit)
        private val ivStatus: ImageView = itemView.findViewById(R.id.ivStatus)

        private var paidWatcher: TextWatcher? = null

        fun bind(record: AdminSummaryItem) {
            tvDay.text = record.date
            tvInvoice.text = String.format(Locale.US, "₹%.2f", record.invoice_amount)

            etPaid.removeTextChangedListener(paidWatcher)
            
            val paidText = if (record.paid_amount != 0.0) String.format(Locale.US, "%.2f", record.paid_amount) else ""
            if (etPaid.text.toString() != paidText) {
                etPaid.setText(paidText)
            }

            updateDueCredit(record)

            paidWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        record.paid_amount = s.toString().toDoubleOrNull() ?: 0.0
                        updateDueCredit(record)
                        onAmountChanged()
                    }
                }
            }

            etPaid.addTextChangedListener(paidWatcher)
        }

        private fun updateDueCredit(record: AdminSummaryItem) {
            val diff = record.invoice_amount - record.paid_amount
            
            when {
                diff == 0.0 -> {
                    tvDueCredit.text = "₹0.00"
                    tvDueCredit.setTextColor(Color.parseColor("#48BB78")) // Green
                    ivStatus.visibility = View.VISIBLE
                }
                diff < 0.0 -> {
                    // Advance
                    tvDueCredit.text = String.format(Locale.US, "₹%.2f adv.", -diff)
                    tvDueCredit.setTextColor(Color.parseColor("#805AD5")) // Purple
                    ivStatus.visibility = View.GONE
                }
                else -> {
                    // Due
                    tvDueCredit.text = String.format(Locale.US, "₹%.2f", diff)
                    tvDueCredit.setTextColor(Color.parseColor("#E53E3E")) // Red
                    ivStatus.visibility = View.GONE
                }
            }
        }
    }
}
