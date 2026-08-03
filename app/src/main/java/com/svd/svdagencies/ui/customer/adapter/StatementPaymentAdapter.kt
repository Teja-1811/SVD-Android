package com.svd.svdagencies.ui.customer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.customer.StatementPayment
import java.text.SimpleDateFormat
import java.util.Locale

class StatementPaymentAdapter : RecyclerView.Adapter<StatementPaymentAdapter.ViewHolder>() {

    private var items: List<StatementPayment> = emptyList()
    private val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    private val outputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)

    fun submitList(newItems: List<StatementPayment>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.customer_statement_payment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMethod: TextView = view.findViewById(R.id.tvPaymentMethod)
        private val tvDate: TextView = view.findViewById(R.id.tvPaymentDate)
        private val tvId: TextView = view.findViewById(R.id.tvTransactionId)
        private val tvAmount: TextView = view.findViewById(R.id.tvPaymentAmount)

        fun bind(item: StatementPayment) {
            tvMethod.text = item.method
            tvId.text = "TXN: ${item.transactionId}"
            tvAmount.text = "₹%.2f".format(item.amount)
            
            try {
                item.completedAt?.let {
                    val date = inputFormat.parse(it)
                    tvDate.text = outputFormat.format(date!!)
                } ?: run {
                    tvDate.text = "Processing"
                }
            } catch (e: Exception) {
                tvDate.text = item.completedAt ?: "N/A"
            }
        }
    }
}
