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
import com.svd.svdagencies.data.model.admin.BillItem
import com.svd.svdagencies.data.model.admin.BillItemDetail

class AdminSummaryAdapter(
    private var items: List<AdminSummaryItem>
) : RecyclerView.Adapter<AdminSummaryAdapter.SummaryViewHolder>() {

    fun updateList(newItems: List<AdminSummaryItem>) {
        items = newItems
        notifyDataSetChanged()
    }
    
    fun getItems(): List<AdminSummaryItem> {
        return items
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.admin_companies_due_daily_row, parent, false)
        return SummaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class SummaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDay: TextView = itemView.findViewById(R.id.tvDay)
        private val etInvoice: EditText = itemView.findViewById(R.id.etInvoice)
        private val etPaid: EditText = itemView.findViewById(R.id.etPaid)

        fun bind(item: AdminSummaryItem) {
            tvDay.text = item.date
            
            // Clear previous listeners to avoid feedback loops and unexpected behaviors during binding
            etInvoice.setOnFocusChangeListener(null)
            etPaid.setOnFocusChangeListener(null)

            if (item.invoice_amount != 0.0) {
                 etInvoice.setText(String.format("%.2f", item.invoice_amount))
            } else {
                 etInvoice.setText("")
            }

            if (item.paid_amount != 0.0) {
                 etPaid.setText(String.format("%.2f", item.paid_amount))
            } else {
                 etPaid.setText("")
            }
            
            // Re-adding Text Watchers to update model correctly
            val invoiceWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val amount = s.toString().toDoubleOrNull() ?: 0.0
                    item.invoice_amount = amount
                }
            }
            etInvoice.addTextChangedListener(invoiceWatcher)

            val paidWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val amount = s.toString().toDoubleOrNull() ?: 0.0
                    item.paid_amount = amount
                }
            }
            etPaid.addTextChangedListener(paidWatcher)
        }
    }
}

class BillItemAdapter(private var items: List<BillItemDetail>) : RecyclerView.Adapter<BillItemAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.admin_bill_info_detail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<BillItemDetail>) {
        items = newItems
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val tvTotal: TextView = itemView.findViewById(R.id.tvTotal)

        fun bind(item: BillItemDetail) {
            tvItemName.text = item.item_name
            tvQuantity.text = "x${item.quantity}"
            tvPrice.text = "@ ₹${item.price_per_unit}"
            tvTotal.text = "= ₹${item.total_amount}"
        }
    }
}

class BillSummaryAdapter(private val items: List<BillItem>) : RecyclerView.Adapter<BillSummaryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.admin_bill_summary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val tvQty: TextView = itemView.findViewById(R.id.tvQty)
        private val tvDisc: TextView = itemView.findViewById(R.id.tvDisc)
        private val tvTotal: TextView = itemView.findViewById(R.id.tvTotal)

        fun bind(item: BillItem) {
            tvProductName.text = item.productName
            tvPrice.text = "₹${item.price}"
            tvQty.text = item.quantity.toString()
            tvDisc.text = "₹${item.discount}"
            tvTotal.text = "₹${item.total_amount}"
        }
    }
}