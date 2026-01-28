package com.svd.svdagencies.ui.admin.bills

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.admin.AdminItem
import com.svd.svdagencies.data.model.admin.BillItemForCreation

class CreateBillItemAdapter(
    private val items: MutableList<BillItemForCreation>,
    private var availableItems: List<AdminItem> = emptyList()
) : RecyclerView.Adapter<CreateBillItemAdapter.ViewHolder>() {

    fun updateAvailableItems(newItems: List<AdminItem>) {
        availableItems = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.admin_bill_summary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    fun addItem(item: BillItemForCreation = BillItemForCreation()) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun removeItem(position: Int) {
        if (position >= 0 && position < items.size) {
            items.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, items.size)
        }
    }

    fun getItems(): List<BillItemForCreation> = items

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val tvQty: TextView = itemView.findViewById(R.id.tvQty)
        private val tvDisc: TextView = itemView.findViewById(R.id.tvDisc)
        private val tvTotal: TextView = itemView.findViewById(R.id.tvTotal)

        fun bind(billItem: BillItemForCreation, position: Int) {
            val item = availableItems.find { it.id == billItem.itemId }
            tvProductName.text = item?.name ?: "Unknown Item"
            tvPrice.text = "₹${item?.selling_price ?: "0.00"}"
            tvQty.text = billItem.quantity.toString()
            tvDisc.text = "₹${billItem.discount}"
            
            val sellingPriceStr = item?.selling_price?.replace("₹", "")?.replace(",", "") ?: "0.00"
            val sellingPrice = sellingPriceStr.toDoubleOrNull() ?: 0.0
            val total = (sellingPrice * billItem.quantity) - billItem.discount
            tvTotal.text = "₹%.2f".format(total)
        }
    }
}
