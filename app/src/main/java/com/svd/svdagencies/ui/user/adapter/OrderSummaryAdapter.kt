package com.svd.svdagencies.ui.user.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.ui.user.model.CartItem

class OrderSummaryAdapter : RecyclerView.Adapter<OrderSummaryAdapter.SummaryViewHolder>() {

    private var items: List<CartItem> = emptyList()

    fun submitList(newItems: List<CartItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_cart_summary_item, parent, false)
        return SummaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class SummaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvItemName)
        private val tvQty: TextView = itemView.findViewById(R.id.tvQty)
        private val tvLineTotal: TextView = itemView.findViewById(R.id.tvLineTotal)

        fun bind(item: CartItem) {
            tvName.text = item.item.name
            tvQty.text = "x${item.quantity}"
            val price = item.unitPrice()
            tvLineTotal.text = String.format("₹ %.2f", price * item.quantity)
        }
    }
}
