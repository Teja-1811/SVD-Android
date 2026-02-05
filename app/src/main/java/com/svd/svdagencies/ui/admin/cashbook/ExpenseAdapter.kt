package com.svd.svdagencies.ui.admin.cashbook

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.data.model.admin.Cashbook.Expense
import com.svd.svdagencies.databinding.AdminExpenseRowBinding

class ExpenseAdapter(
    private var items: List<Expense>,
    private val onEdit: (Expense) -> Unit,
    private val onDelete: (Expense) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ViewHolder>() {

    class ViewHolder(val binding: AdminExpenseRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AdminExpenseRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            tvCategory.text = item.category
            tvDescription.text = item.description
            tvDate.text = item.date
            tvAmount.text = "₹ %.2f".format(item.amount)

            btnEdit.setOnClickListener { onEdit(item) }
            btnDelete.setOnClickListener { onDelete(item) }
        }
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<Expense>) {
        items = newItems
        notifyDataSetChanged()
    }
}
