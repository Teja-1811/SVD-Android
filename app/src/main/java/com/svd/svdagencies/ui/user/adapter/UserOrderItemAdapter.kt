package com.svd.svdagencies.ui.user.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.user.UserOrderItem

class UserOrderItemAdapter(
    private val items: List<UserOrderItem>
) : RecyclerView.Adapter<UserOrderItemAdapter.ItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_order_item_row, parent, false)
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
        private val tvRequested: TextView = itemView.findViewById(R.id.tvRequestedInfo)
        private val tvApproved: TextView = itemView.findViewById(R.id.tvApprovedInfo)
        private val tvDiscount: TextView = itemView.findViewById(R.id.tvItemDiscount)

        fun bind(item: UserOrderItem) {
            tvName.text = item.name
            
            // If order is approved/shipped, show approved total, else requested total
            val displayTotal = if (item.approvedTotal > 0) item.approvedTotal else item.requestedTotal
            tvTotal.text = "₹%.2f".format(displayTotal)
            
            tvRequested.text = "Req: ${item.requestedQuantity} x ₹%.2f".format(item.requestedPrice)
            
            if (item.approvedQuantity > 0) {
                tvApproved.visibility = View.VISIBLE
                tvApproved.text = "Appr: ${item.approvedQuantity} x ₹%.2f".format(item.approvedPrice)
            } else {
                tvApproved.visibility = View.GONE
            }

            if (item.discountTotal > 0) {
                tvDiscount.visibility = View.VISIBLE
                tvDiscount.text = "Disc: ₹%.2f".format(item.discountTotal)
            } else {
                tvDiscount.visibility = View.GONE
            }
        }
    }
}
