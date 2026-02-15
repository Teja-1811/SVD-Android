package com.svd.svdagencies.ui.admin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.data.model.admin.CustomerPaymentItem
import com.svd.svdagencies.databinding.AdminCustomerPaymentRowBinding

class CustomerPaymentAdapter(
    private val onUpdateStatus: (CustomerPaymentItem) -> Unit,
    private val onDelete: (CustomerPaymentItem) -> Unit
) : RecyclerView.Adapter<CustomerPaymentAdapter.ViewHolder>() {

    private var payments = listOf<CustomerPaymentItem>()

    fun setData(newPayments: List<CustomerPaymentItem>) {
        payments = newPayments
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AdminCustomerPaymentRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(payments[position])
    }

    override fun getItemCount(): Int = payments.size

    inner class ViewHolder(private val binding: AdminCustomerPaymentRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CustomerPaymentItem) {
            binding.tvCustomer.text = item.customer_name ?: "N/A"
            binding.tvAmount.text = "₹${item.amount}"
            binding.tvTxnId.text = item.transaction_id ?: "N/A"
            binding.tvMethod.text = item.payment_mode ?: "N/A"
            binding.tvStatus.text = item.status ?: "N/A"
            binding.tvDate.text = item.created_at ?: "N/A"

            binding.btnEditStatus.setOnClickListener { onUpdateStatus(item) }
            binding.btnDelete.setOnClickListener { onDelete(item) }
        }
    }
}
