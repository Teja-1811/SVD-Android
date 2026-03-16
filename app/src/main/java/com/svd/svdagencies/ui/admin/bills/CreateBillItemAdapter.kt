package com.svd.svdagencies.ui.admin.bills

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.admin.Items.AdminItem
import com.svd.svdagencies.data.model.admin.Bills.BillItemForCreation

class CreateBillItemAdapter(
    private val items: MutableList<BillItemForCreation>,
    private var availableItems: List<AdminItem> = emptyList(),
    private val onRemoveClick: (Int) -> Unit
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

    fun addItem(newItem: BillItemForCreation) {
        val existingItemIndex = items.indexOfFirst { it.itemId == newItem.itemId }
        if (existingItemIndex != -1) {
            // Replace the existing item with the new one (with the new quantity)
            items[existingItemIndex] = newItem
            notifyItemChanged(existingItemIndex)
        } else {
            items.add(newItem)
            notifyItemInserted(items.size - 1)
        }
    }

    fun removeItem(position: Int) {
        if (position >= 0 && position < items.size) {
            items.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, items.size)
        }
    }

    fun updateItems(newItems: List<BillItemForCreation>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun getItems(): List<BillItemForCreation> = items

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val tvQty: TextView = itemView.findViewById(R.id.tvQty)
        private val tvDisc: TextView = itemView.findViewById(R.id.tvDisc)
        private val tvDiscAmount: TextView = itemView.findViewById(R.id.tvDiscAmount)
        private val tvTotal: TextView = itemView.findViewById(R.id.tvTotal)
        private val btnRemove = itemView.findViewById<MaterialButton>(R.id.btnRemove)

        fun bind(billItem: BillItemForCreation, position: Int) {
            val item = availableItems.find { it.id == billItem.itemId }
            
            val productName = item?.name ?: billItem.itemName ?: "Unknown Item"
            tvProductName.text = productName
            
            val sellingPriceStr = item?.selling_price?.replace("₹", "")?.replace(",", "") 
                ?: billItem.price?.toString() 
                ?: "0.00"
            
            tvPrice.text = "₹$sellingPriceStr"
            tvQty.text = billItem.quantity.toString()
            tvDisc.text = "₹${billItem.discount}"
            tvDiscAmount.text = "₹%.2f".format(billItem.totalDiscount)
            
            val sellingPrice = sellingPriceStr.toDoubleOrNull() ?: 0.0
            val total = (sellingPrice * billItem.quantity) - billItem.totalDiscount
            tvTotal.text = "₹%.2f".format(total)

            btnRemove.setOnClickListener { onRemoveClick(position) }
        }
    }
}
