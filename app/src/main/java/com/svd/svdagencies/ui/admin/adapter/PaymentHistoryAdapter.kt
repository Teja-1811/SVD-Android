package com.svd.svdagencies.ui.admin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.databinding.AdminPaymentHistoryRowBinding

// Assuming we have a Payment model. If not, we'll use a Map or create one.
// Let's use a simple data class for now or a Map if model is not found.
data class PaymentRecord(
    val customer: String,
    val amount: Double,
    val method: String,
    val txnId: String?,
    val date: String
)

class PaymentHistoryAdapter(private var payments: List<PaymentRecord>) : 
    RecyclerView.Adapter<PaymentHistoryAdapter.ViewHolder>() {

    class ViewHolder(val binding: AdminPaymentHistoryRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AdminPaymentHistoryRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val payment = payments[position]
        holder.binding.apply {
            layoutHeader.visibility = if (position == 0) View.VISIBLE else View.GONE
            
            tvCustomerName.text = payment.customer
            tvAmount.text = "Rs. ${payment.amount}"
            tvMethod.text = payment.method
            tvTxnId.text = payment.txnId ?: "N/A"
            tvDate.text = payment.date
        }
    }

    override fun getItemCount() = payments.size

    fun updateData(newPayments: List<PaymentRecord>) {
        payments = newPayments
        notifyDataSetChanged()
    }
}
