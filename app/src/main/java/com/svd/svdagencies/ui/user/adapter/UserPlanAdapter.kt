package com.svd.svdagencies.ui.user.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.user.UserPlan
import com.svd.svdagencies.data.model.user.UserSubscriptionItem
import java.util.Locale

class UserPlanAdapter(
    private var plans: List<UserPlan> = emptyList(),
    private val onViewPlan: (UserPlan) -> Unit,
    private val onActivatePlan: (UserPlan) -> Unit
) : RecyclerView.Adapter<UserPlanAdapter.PlanViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_plan_card, parent, false)
        return PlanViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        holder.bind(plans[position])
    }

    override fun getItemCount(): Int = plans.size

    fun submitList(newPlans: List<UserPlan>) {
        plans = newPlans
        notifyDataSetChanged()
    }

    inner class PlanViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName = view.findViewById<TextView>(R.id.tvPlanName)
        private val tvTag = view.findViewById<TextView>(R.id.tvPlanTag)
        private val tvPrice = view.findViewById<TextView>(R.id.tvPrice)
        private val tvFrequency = view.findViewById<TextView>(R.id.tvFrequency)
        private val tvDescription = view.findViewById<TextView>(R.id.tvDescription)
        private val tvItemsSummary = view.findViewById<TextView>(R.id.tvItemsSummary)
        private val tvItemsSecondary = view.findViewById<TextView>(R.id.tvItemsSecondary)
        private val btnViewPlan = view.findViewById<MaterialButton>(R.id.btnViewPlan)
        private val btnActivate = view.findViewById<MaterialButton>(R.id.btnActivate)

        fun bind(plan: UserPlan) {
            tvName.text = plan.name
            tvTag.text = "Plan ID: ${plan.id}"
            tvPrice.text = formatCurrency(plan.price)
            tvFrequency.text = formatFrequency(plan.price)

            if (plan.description.isNullOrBlank()) {
                tvDescription.visibility = View.GONE
            } else {
                tvDescription.visibility = View.VISIBLE
                tvDescription.text = plan.description
            }

            bindItems(plan.items)
            btnViewPlan.setOnClickListener { onViewPlan(plan) }
            btnActivate.setOnClickListener { onActivatePlan(plan) }
        }

        private fun formatCurrency(value: Double): String {
            return String.format(Locale.getDefault(), "\u20B9 %.0f/month", value)
        }

        private fun formatFrequency(value: Double): String {
            val perDelivery = if (value > 0) value / 4 else 0.0
            return String.format(Locale.getDefault(), "\u20B9 %.0f per delivery", perDelivery)
        }

        private fun bindItems(items: List<UserSubscriptionItem>?) {
            if (items.isNullOrEmpty()) {
                tvItemsSummary.text = "Includes: Items will appear here"
                tvItemsSecondary.visibility = View.GONE
                return
            }

            val primary = items.first()
            val remaining = items.drop(1)
            tvItemsSummary.text = "Includes: ${primary.itemName} (${formatQuantity(primary.quantity)})" +
                    primary.price?.let { " • Rs. ${formatNumber(it)}/${primary.per ?: "day"}" } ?: ""
            if (remaining.isNotEmpty()) {
                val more = remaining.joinToString {
                    val qty = formatQuantity(it.quantity)
                    val priceText = it.price?.let { p -> " • Rs. ${formatNumber(p)}/${it.per ?: "day"}" } ?: ""
                    "${it.itemName} ($qty)$priceText"
                }
                tvItemsSecondary.visibility = View.VISIBLE
                tvItemsSecondary.text = "• $more"
            } else {
                tvItemsSecondary.visibility = View.GONE
            }
        }

        private fun formatQuantity(value: Double): String {
            return if (value % 1.0 == 0.0) {
                value.toInt().toString()
            } else {
                String.format(Locale.getDefault(), "%.2f", value)
            }
        }

        private fun formatNumber(value: Double): String {
            return if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.getDefault(), "%.2f", value)
        }
    }
}
