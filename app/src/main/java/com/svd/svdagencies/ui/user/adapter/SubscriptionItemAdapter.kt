package com.svd.svdagencies.ui.user.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.user.UserSubscriptionItem

class SubscriptionItemAdapter : RecyclerView.Adapter<SubscriptionItemAdapter.ItemViewHolder>() {

    private val items = mutableListOf<UserSubscriptionItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_subscription_item_row, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun submitList(list: List<UserSubscriptionItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val itemName = view.findViewById<TextView>(R.id.tvItemName)
        private val quantity = view.findViewById<TextView>(R.id.tvQuantity)

        fun bind(item: UserSubscriptionItem) {
            itemName.text = item.itemName
            quantity.text = "Qty: ${formatQuantity(item.quantity)}"
        }

        private fun formatQuantity(quantity: Double): String {
            return if (quantity % 1.0 == 0.0) {
                quantity.toInt().toString()
            } else {
                quantity.toString()
            }
        }
    }
}
