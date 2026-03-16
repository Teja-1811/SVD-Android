package com.svd.svdagencies.ui.admin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.data.model.admin.SubscriptionPlan
import com.svd.svdagencies.data.model.admin.SubscriptionPlanItem
import com.svd.svdagencies.databinding.AdminSubscriptionPlanCardBinding

class SubscriptionPlanAdapter(
    private var plans: List<SubscriptionPlan>,
    private val onEditPlan: (SubscriptionPlan) -> Unit,
    private val onAddItem: (SubscriptionPlan) -> Unit,
    private val onEditItem: (SubscriptionPlan, SubscriptionPlanItem) -> Unit,
    private val onDeleteItem: (SubscriptionPlan, SubscriptionPlanItem) -> Unit
) : RecyclerView.Adapter<SubscriptionPlanAdapter.ViewHolder>() {

    private val viewPool = RecyclerView.RecycledViewPool()

    inner class ViewHolder(val binding: AdminSubscriptionPlanCardBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.rvPlanItems.layoutManager = LinearLayoutManager(binding.root.context)
            binding.rvPlanItems.setRecycledViewPool(viewPool)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AdminSubscriptionPlanCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val plan = plans[position]
        holder.binding.apply {
            tvPlanName.text = plan.name
            tvPrice.text = "Total: Rs. ${plan.price}"
            tvDuration.text = "Duration: ${plan.durationInDays} days"
            tvDescription.text = plan.description ?: ""
            tvDescription.visibility = if (plan.description.isNullOrEmpty()) View.GONE else View.VISIBLE

            val itemsAdapter = PlanItemAdapter(plan.items ?: emptyList(), { item ->
                onEditItem(plan, item)
            }, { item ->
                onDeleteItem(plan, item)
            })
            rvPlanItems.adapter = itemsAdapter

            btnEditPlan.setOnClickListener { onEditPlan(plan) }
            btnAddItem.setOnClickListener { onAddItem(plan) }
        }
    }

    override fun getItemCount() = plans.size

    fun updateData(newPlans: List<SubscriptionPlan>) {
        plans = newPlans
        notifyDataSetChanged()
    }
}
