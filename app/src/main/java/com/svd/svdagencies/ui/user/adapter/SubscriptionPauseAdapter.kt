package com.svd.svdagencies.ui.user.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.user.UserSubscriptionPause

class SubscriptionPauseAdapter : RecyclerView.Adapter<SubscriptionPauseAdapter.PauseViewHolder>() {

    private val pauses = mutableListOf<UserSubscriptionPause>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PauseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_subscription_pause_item, parent, false)
        return PauseViewHolder(view)
    }

    override fun onBindViewHolder(holder: PauseViewHolder, position: Int) {
        holder.bind(pauses[position])
    }

    override fun getItemCount(): Int = pauses.size

    fun submitList(list: List<UserSubscriptionPause>) {
        pauses.clear()
        pauses.addAll(list)
        notifyDataSetChanged()
    }

    inner class PauseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val plan = view.findViewById<TextView>(R.id.tvPausePlan)
        private val dates = view.findViewById<TextView>(R.id.tvPauseDates)
        private val reason = view.findViewById<TextView>(R.id.tvPauseReason)
        private val status = view.findViewById<TextView>(R.id.tvPauseStatus)

        fun bind(pause: UserSubscriptionPause) {
            plan.text = pause.plan
            dates.text = pause.pauseDate?.substringBefore("T")?.let { "Paused on $it" } ?: "Paused date not available"
            reason.text = pause.reason?.let { "Reason: $it" } ?: "Reason not provided"
            status.text = pause.status
        }
    }
}
