package com.svd.svdagencies.ui.customer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.delivery.DeliveryCustomerPaymentRecord
import com.svd.svdagencies.databinding.CustomerPaymentRowBinding
import java.text.SimpleDateFormat
import java.util.Locale

class CustomerPaymentHistoryAdapter : RecyclerView.Adapter<CustomerPaymentHistoryAdapter.ViewHolder>() {

    private var items: List<DeliveryCustomerPaymentRecord> = emptyList()
    private val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    private val outputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)

    fun submitList(newItems: List<DeliveryCustomerPaymentRecord>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CustomerPaymentRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: CustomerPaymentRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DeliveryCustomerPaymentRecord) {
            binding.tvAmount.text = "₹%.2f".format(item.amount)
            binding.tvPaymentFor.text = item.paymentFor ?: "Payment"
            binding.tvPaymentMethod.text = "Method: ${item.method ?: "N/A"}"
            binding.tvTransactionId.text = "TXN: ${item.transactionId ?: "N/A"}"
            binding.tvStatus.text = item.status?.uppercase() ?: "UNKNOWN"

            val statusBg = when (item.status?.lowercase()) {
                "success", "paid", "completed" -> R.drawable.bg_status_green
                "pending", "processing" -> R.drawable.bg_status_yellow
                "failed", "cancelled" -> R.drawable.bg_status_red
                else -> R.drawable.bg_status_yellow
            }
            binding.tvStatus.setBackgroundResource(statusBg)

            try {
                item.createdAt?.let {
                    val date = inputFormat.parse(it)
                    binding.tvPaymentDate.text = outputFormat.format(date!!)
                }
            } catch (e: Exception) {
                binding.tvPaymentDate.text = item.createdAt ?: "N/A"
            }
        }
    }
}
