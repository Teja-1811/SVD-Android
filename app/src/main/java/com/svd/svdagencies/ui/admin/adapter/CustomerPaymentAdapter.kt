package com.svd.svdagencies.ui.admin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.data.model.admin.CustomerPaymentItem
import com.svd.svdagencies.databinding.AdminCustomerPaymentRowBinding

class CustomerPaymentAdapter(
    private val onUpdateStatus: (CustomerPaymentItem) -> Unit,
    private val onMarkSuccess: (CustomerPaymentItem) -> Unit,
    private val onMarkFailure: (CustomerPaymentItem) -> Unit,
    private val onDelete: (CustomerPaymentItem) -> Unit,
    private val onWhatsAppShare: (CustomerPaymentItem) -> Unit
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
            binding.tvCustomerPhone.text = item.customer_phone ?: "N/A"
            binding.tvAmount.text = "₹${item.amount}"
            binding.tvTxnId.text = item.transaction_id ?: "N/A"
            binding.tvMethod.text = item.payment_mode ?: "N/A"
            binding.tvStatus.text = item.status ?: "N/A"
            binding.tvStatusBadge.text = item.status ?: "N/A"
            binding.tvDate.text = item.created_at ?: "N/A"

            val normalizedStatus = item.status?.lowercase().orEmpty()
            when (normalizedStatus) {
                "success" -> {
                    binding.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#E0F2F1")
                    )
                    binding.tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#00695C"))
                }
                "pending" -> {
                    binding.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#FFF7D6")
                    )
                    binding.tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#A16207"))
                }
                else -> {
                    binding.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#FFEBEE")
                    )
                    binding.tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#C62828"))
                }
            }

            val isPending = normalizedStatus == "pending"
            binding.btnMarkSuccess.visibility = if (isPending) View.VISIBLE else View.GONE
            binding.spaceSuccessFailure.visibility = if (isPending) View.VISIBLE else View.GONE
            binding.btnMarkFailure.visibility = if (isPending) View.VISIBLE else View.GONE
            binding.spaceFailureEdit.visibility = if (isPending) View.GONE else View.GONE
            binding.btnEditStatus.visibility = if (isPending) View.GONE else View.VISIBLE

            binding.btnEditStatus.setOnClickListener { onUpdateStatus(item) }
            binding.btnMarkSuccess.setOnClickListener { onMarkSuccess(item) }
            binding.btnMarkFailure.setOnClickListener { onMarkFailure(item) }
            binding.btnDelete.setOnClickListener { onDelete(item) }
            binding.btnWhatsApp.setOnClickListener { onWhatsAppShare(item) }
        }
    }
}
