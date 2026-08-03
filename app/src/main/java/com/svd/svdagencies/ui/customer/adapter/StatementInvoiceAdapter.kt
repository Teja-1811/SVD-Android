package com.svd.svdagencies.ui.customer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.customer.StatementInvoice
import java.text.SimpleDateFormat
import java.util.Locale

class StatementInvoiceAdapter : RecyclerView.Adapter<StatementInvoiceAdapter.ViewHolder>() {

    private var items: List<StatementInvoice> = emptyList()
    private val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

    fun submitList(newItems: List<StatementInvoice>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.customer_statement_invoice, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvNumber: TextView = view.findViewById(R.id.tvInvoiceNumber)
        private val tvDate: TextView = view.findViewById(R.id.tvInvoiceDate)
        private val tvAmount: TextView = view.findViewById(R.id.tvInvoiceAmount)

        fun bind(item: StatementInvoice) {
            tvNumber.text = item.invoiceNumber
            tvAmount.text = "₹%.2f".format(item.totalAmount)
            
            try {
                val date = inputFormat.parse(item.invoiceDate)
                tvDate.text = outputFormat.format(date!!)
            } catch (e: Exception) {
                tvDate.text = item.invoiceDate
            }
        }
    }
}
