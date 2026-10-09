package com.svd.svdagencies.ui.admin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.admin.AdminEnquiry

class AdminEnquiriesAdapter(
    private val onResolve: (AdminEnquiry) -> Unit
) : RecyclerView.Adapter<AdminEnquiriesAdapter.EnquiryViewHolder>() {

    private val enquiries = mutableListOf<AdminEnquiry>()
    private var resolvedMode: Boolean = false

    fun submitList(items: List<AdminEnquiry>, resolvedMode: Boolean) {
        this.resolvedMode = resolvedMode
        enquiries.clear()
        enquiries.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EnquiryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.admin_enquiry_card, parent, false)
        return EnquiryViewHolder(view)
    }

    override fun onBindViewHolder(holder: EnquiryViewHolder, position: Int) {
        holder.bind(enquiries[position], resolvedMode)
    }

    override fun getItemCount(): Int = enquiries.size

    inner class EnquiryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSubject: TextView = itemView.findViewById(R.id.tvSubject)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvPhone: TextView = itemView.findViewById(R.id.tvPhone)
        private val tvEmail: TextView = itemView.findViewById(R.id.tvEmail)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val btnResolve: MaterialButton = itemView.findViewById(R.id.btnResolve)
        private val tvStatusBadge: TextView = itemView.findViewById(R.id.tvStatusBadge)
        private val layoutActions: View = itemView.findViewById(R.id.layoutActions)

        fun bind(enquiry: AdminEnquiry, resolvedMode: Boolean) {
            tvSubject.text = enquiry.subject
            tvDate.text = enquiry.createdAt
            tvName.text = enquiry.name
            tvPhone.text = enquiry.phone
            tvMessage.text = enquiry.message

            if (enquiry.email.isBlank()) {
                tvEmail.visibility = View.GONE
            } else {
                tvEmail.visibility = View.VISIBLE
                tvEmail.text = enquiry.email
            }

            if (resolvedMode) {
                layoutActions.visibility = View.GONE
                tvStatusBadge.visibility = View.VISIBLE
                tvStatusBadge.text = enquiry.status.uppercase()
            } else {
                layoutActions.visibility = View.VISIBLE
                tvStatusBadge.visibility = View.GONE
                btnResolve.setOnClickListener { onResolve(enquiry) }
            }
        }
    }
}
