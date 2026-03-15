package com.svd.svdagencies.ui.user.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.user.UserSubscriptionHistory

class SubscriptionHistoryAdapter : RecyclerView.Adapter<SubscriptionHistoryAdapter.HistoryViewHolder>() {

    private val history = mutableListOf<UserSubscriptionHistory>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_subscription_history_item, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(history[position])
    }

    override fun getItemCount(): Int = history.size

    fun submitList(list: List<UserSubscriptionHistory>) {
        history.clear()
        history.addAll(list)
        notifyDataSetChanged()
    }

    inner class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val planName = view.findViewById<TextView>(R.id.tvHistoryPlan)
        private val dates = view.findViewById<TextView>(R.id.tvHistoryDates)
        private val status = view.findViewById<TextView>(R.id.tvHistoryStatus)

        fun bind(historyItem: UserSubscriptionHistory) {
            planName.text = historyItem.plan
            dates.text = formatRange(historyItem.startDate, historyItem.endDate)
            status.text = historyItem.status
            status.setTextColor(
                itemView.context.getColor(
                    if (historyItem.status.equals("Active", ignoreCase = true)) {
                        R.color.icon_green
                    } else {
                        R.color.textColorSecondary
                    }
                )
            )
        }

        private fun formatRange(start: String?, end: String?): String {
            val startLabel = start?.substringBefore("T") ?: "Start"
            val endLabel = end?.substringBefore("T") ?: "End"
            return "$startLabel — $endLabel"
        }
    }
}
