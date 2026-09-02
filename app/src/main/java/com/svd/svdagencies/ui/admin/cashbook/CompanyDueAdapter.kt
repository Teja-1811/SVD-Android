package com.svd.svdagencies.ui.admin.cashbook

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.data.model.admin.Cashbook.CompanyDue
import com.svd.svdagencies.databinding.AdminCashbookCompanyDueBinding

class CompanyDueAdapter(private var items: List<CompanyDue>) :
    RecyclerView.Adapter<CompanyDueAdapter.ViewHolder>() {

    class ViewHolder(val binding: AdminCashbookCompanyDueBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AdminCashbookCompanyDueBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            tvCompanyName.text = item.companyName
            tvInvoice.text = "₹%.2f".format(item.invoice)
            tvPaid.text = "₹%.2f".format(item.paid)
            tvDue.text = "₹%.2f".format(item.due)
        }
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<CompanyDue>) {
        items = newItems
        notifyDataSetChanged()
    }
}
