package com.svd.svdagencies.ui.delivery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.admin.Bills.BillItemDetail
import java.util.Locale

class BillDetailItemAdapter(
    private var items: List<BillItemDetail> = emptyList()
) : RecyclerView.Adapter<BillDetailItemAdapter.ViewHolder>() {

    fun submitList(newList: List<BillItemDetail>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.admin_bill_info_detail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position + 1)
    }

    override fun getItemCount() = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvIndex: TextView = view.findViewById(R.id.tvIndex)
        private val tvName: TextView = view.findViewById(R.id.tvItemName)
        private val tvQty: TextView = view.findViewById(R.id.tvQuantity)
        private val tvLineTotal: TextView = view.findViewById(R.id.tvTotal)

        fun bind(item: BillItemDetail, index: Int) {
            tvIndex.text = index.toString()
            tvName.text = item.item_name
            tvQty.text = String.format(Locale.getDefault(), "%d", item.quantity)
            tvLineTotal.text = String.format(Locale.getDefault(), "₹ %.2f", item.total_amount)
        }
    }
}
