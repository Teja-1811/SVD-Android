package com.svd.svdagencies.ui.admin.cashbook

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.admin.CompanyDue
import com.svd.svdagencies.databinding.AdminCompaniesDueRowBinding

class CompanyDueAdapter(private var items: List<CompanyDue>) :
    RecyclerView.Adapter<CompanyDueAdapter.ViewHolder>() {

    class ViewHolder(val binding: AdminCompaniesDueRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AdminCompaniesDueRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            tvDate.text = item.company_name
            tvInvoice.text = "Inv: ₹%.2f".format(item.total_invoice)
            tvPaid.text = "Due: ₹%.2f".format(item.total_due)
            tvPaid.setTextColor(ContextCompat.getColor(root.context, R.color.brand_red))
        }
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<CompanyDue>) {
        items = newItems
        notifyDataSetChanged()
    }
}
