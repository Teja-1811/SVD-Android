package com.svd.svdagencies.ui.admin.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.admin.AdminSummaryItem

class AdminSummaryAdapter(
    private var items: List<AdminSummaryItem>
) : RecyclerView.Adapter<AdminSummaryAdapter.SummaryViewHolder>() {

    fun updateList(newItems: List<AdminSummaryItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_monthly_summary, parent, false)
        return SummaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class SummaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvInvoice: TextView = itemView.findViewById(R.id.tvInvoice)
        private val tvPaid: TextView = itemView.findViewById(R.id.tvPaid)

        fun bind(item: AdminSummaryItem) {
            tvDate.text = item.date
            
            // Format to show 0 if value is 0, else 2 decimal places
            tvInvoice.text = formatAmount(item.invoice_amount)
            tvPaid.text = formatAmount(item.paid_amount)
        }

        private fun formatAmount(amount: Double): String {
            return if (amount == 0.0) {
                "0"
            } else {
                String.format("%.2f", amount)
            }
        }
    }
}
