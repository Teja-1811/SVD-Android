package com.svd.svdagencies.ui.admin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.databinding.AdminSubscriptionRowBinding
import com.svd.svdagencies.data.model.admin.CustomerSubscription

class SubscriptionRowAdapter(
    private var subscriptions: List<CustomerSubscription>,
    private val type: SubscriptionType,
    private val onPay: (CustomerSubscription) -> Unit,
    private val onToggle: (CustomerSubscription) -> Unit
) : RecyclerView.Adapter<SubscriptionRowAdapter.ViewHolder>() {

    enum class SubscriptionType { ACTIVE, DEACTIVATED, EXPIRED, EXPIRING_SOON }

    inner class ViewHolder(val binding: AdminSubscriptionRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AdminSubscriptionRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sub = subscriptions[position]
        holder.binding.apply {
            // Only show header for the first item
            layoutHeader.visibility = if (position == 0) View.VISIBLE else View.GONE
            
            tvCustomerName.text = sub.customer
            tvPlanName.text = sub.plan
            tvStartDate.text = sub.startDate
            tvEndDate.text = sub.endDate

            // Configure actions based on type
            when (type) {
                SubscriptionType.ACTIVE -> {
                    btnPay.visibility = View.VISIBLE
                    btnPay.text = "Pay"
                    btnDeactivate.visibility = View.VISIBLE
                    btnDeactivate.text = "Deactivate"
                }
                SubscriptionType.DEACTIVATED -> {
                    btnPay.visibility = View.VISIBLE
                    btnPay.text = "Activate"
                    btnDeactivate.visibility = View.GONE
                }
                SubscriptionType.EXPIRED, SubscriptionType.EXPIRING_SOON -> {
                    layoutActions.visibility = View.GONE
                }
            }

            btnPay.setOnClickListener { 
                if (type == SubscriptionType.DEACTIVATED) onToggle(sub) else onPay(sub)
            }
            btnDeactivate.setOnClickListener { onToggle(sub) }
        }
    }

    override fun getItemCount() = subscriptions.size

    fun updateData(newSubs: List<CustomerSubscription>) {
        this.subscriptions = newSubs
        notifyDataSetChanged()
    }
}
