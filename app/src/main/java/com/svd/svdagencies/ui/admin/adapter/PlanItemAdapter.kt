package com.svd.svdagencies.ui.admin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.databinding.AdminSubscriptionPlanItemRowBinding
import com.svd.svdagencies.data.model.admin.SubscriptionPlanItem

class PlanItemAdapter(
    private var items: List<SubscriptionPlanItem>,
    private val onEdit: (SubscriptionPlanItem) -> Unit,
    private val onDelete: (SubscriptionPlanItem) -> Unit
) : RecyclerView.Adapter<PlanItemAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: AdminSubscriptionPlanItemRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AdminSubscriptionPlanItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            tvItemName.text = item.itemName
            tvQuantity.text = item.quantity.toString()
            btnEditItem.setOnClickListener { onEdit(item) }
            btnDeleteItem.setOnClickListener { onDelete(item) }
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<SubscriptionPlanItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}
