package com.svd.svdagencies.ui.user.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.user.UserBillItem

class UserBillItemAdapter(
    private val items: List<UserBillItem>
) : RecyclerView.Adapter<UserBillItemAdapter.ItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_bill_item, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvItemName)
        private val tvTotal: TextView = itemView.findViewById(R.id.tvItemTotal)
        private val tvDetails: TextView = itemView.findViewById(R.id.tvItemDetails)
        private val tvDiscount: TextView = itemView.findViewById(R.id.tvItemDiscount)

        fun bind(item: UserBillItem) {
            tvName.text = item.name
            tvTotal.text = "₹%.2f".format(item.totalAmount)
            tvDetails.text = "Qty: ${item.quantity} | Price: ₹%.2f".format(item.pricePerUnit)
            
            if (item.discount > 0) {
                tvDiscount.visibility = View.VISIBLE
                tvDiscount.text = "Disc: ₹%.2f".format(item.discount)
            } else {
                tvDiscount.visibility = View.GONE
            }
        }
    }
}
