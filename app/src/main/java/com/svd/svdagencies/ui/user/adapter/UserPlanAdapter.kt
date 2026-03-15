package com.svd.svdagencies.ui.user.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.user.UserPlan
import java.util.Locale

class UserPlanAdapter(
    private val onSubscribe: (UserPlan) -> Unit
) : RecyclerView.Adapter<UserPlanAdapter.PlanViewHolder>() {

    private val plans = mutableListOf<UserPlan>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_plan_card, parent, false)
        return PlanViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        holder.bind(plans[position])
    }

    override fun getItemCount(): Int = plans.size

    fun submitList(list: List<UserPlan>) {
        plans.clear()
        plans.addAll(list)
        notifyDataSetChanged()
    }

    inner class PlanViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName = view.findViewById<TextView>(R.id.tvPlanName)
        private val tvTag = view.findViewById<TextView>(R.id.tvPlanTag)
        private val tvPrice = view.findViewById<TextView>(R.id.tvPrice)
        private val tvDescription = view.findViewById<TextView>(R.id.tvDescription)
        private val btnSubscribe = view.findViewById<MaterialButton>(R.id.btnSubscribe)

        fun bind(plan: UserPlan) {
            tvName.text = plan.name
            tvTag.text = "Plan ID: ${plan.id}"
            tvPrice.text = formatCurrency(plan.price)
            tvDescription.text = plan.description
            btnSubscribe.setOnClickListener {
                onSubscribe(plan)
            }
        }

        private fun formatCurrency(value: Double): String {
            return String.format(Locale.getDefault(), "₹ %.2f", value)
        }
    }
}
