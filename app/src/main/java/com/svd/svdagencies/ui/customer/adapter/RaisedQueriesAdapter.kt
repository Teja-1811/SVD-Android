package com.svd.svdagencies.ui.customer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.customer.RaisedQuery

class RaisedQueriesAdapter : RecyclerView.Adapter<RaisedQueriesAdapter.QueryViewHolder>() {

    private val queries = mutableListOf<RaisedQuery>()

    fun submitList(items: List<RaisedQuery>) {
        queries.clear()
        queries.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.customer_raised_query_item, parent, false)
        return QueryViewHolder(view)
    }

    override fun onBindViewHolder(holder: QueryViewHolder, position: Int) {
        holder.bind(queries[position])
    }

    override fun getItemCount(): Int = queries.size

    inner class QueryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSubject: TextView = itemView.findViewById(R.id.tvQuerySubject)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvQueryStatus)
        private val tvCreatedAt: TextView = itemView.findViewById(R.id.tvQueryCreatedAt)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvQueryMessage)
        private val tvContact: TextView = itemView.findViewById(R.id.tvQueryContact)

        fun bind(query: RaisedQuery) {
            tvSubject.text = query.subject
            tvStatus.text = query.status
            tvCreatedAt.text = query.createdAt
            tvMessage.text = query.message
            tvContact.text = if (query.email.isNotBlank()) {
                itemView.context.getString(R.string.query_contact_with_email, query.phone, query.email)
            } else {
                query.phone
            }

            when (query.status.lowercase()) {
                "resolved" -> tvStatus.setBackgroundResource(R.drawable.bg_status_green)
                "active" -> tvStatus.setBackgroundResource(R.drawable.bg_status_yellow)
                else -> tvStatus.setBackgroundResource(R.drawable.bg_status_red)
            }
        }
    }
}
