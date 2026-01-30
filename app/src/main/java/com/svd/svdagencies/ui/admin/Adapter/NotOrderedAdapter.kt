package com.svd.svdagencies.ui.admin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.data.model.admin.CustomerItem
import com.svd.svdagencies.databinding.AdminNoOrderCustomerBinding

class NotOrderedAdapter(
    private var items: List<CustomerItem>,
    private val onCallClick: (CustomerItem) -> Unit
) : RecyclerView.Adapter<NotOrderedAdapter.ViewHolder>() {

    class ViewHolder(val binding: AdminNoOrderCustomerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AdminNoOrderCustomerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.txtCustomerName.text = item.name
        holder.binding.btnCall.setOnClickListener { onCallClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<CustomerItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
