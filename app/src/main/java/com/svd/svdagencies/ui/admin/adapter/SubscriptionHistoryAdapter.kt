package com.svd.svdagencies.ui.admin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.data.model.admin.CustomerSubscription
import com.svd.svdagencies.databinding.AdminSubscriptionHistoryRowBinding

class SubscriptionHistoryAdapter(private var subscriptions: List<CustomerSubscription>) : 
    RecyclerView.Adapter<SubscriptionHistoryAdapter.ViewHolder>() {

    class ViewHolder(val binding: AdminSubscriptionHistoryRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AdminSubscriptionHistoryRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sub = subscriptions[position]
        holder.binding.apply {
            layoutHeader.visibility = if (position == 0) View.VISIBLE else View.GONE
            
            tvCustomerName.text = sub.customer
            tvPlanName.text = sub.plan
            tvStartDate.text = sub.startDate
            tvEndDate.text = sub.endDate
            tvStatus.text = if (sub.isActive) "Active" else "Inactive"
            tvStatus.setTextColor(root.context.getColor(if (sub.isActive) android.R.color.holo_green_dark else android.R.color.holo_red_dark))
        }
    }

    override fun getItemCount() = subscriptions.size

    fun updateData(newSubs: List<CustomerSubscription>) {
        subscriptions = newSubs
        notifyDataSetChanged()
    }
}
