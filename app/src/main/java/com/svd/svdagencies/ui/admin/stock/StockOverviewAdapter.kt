package com.svd.svdagencies.ui.admin.stock

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.admin.stock.StockItem
import java.util.Locale

class StockOverviewAdapter(private var items: List<StockItem>) :
    RecyclerView.Adapter<StockOverviewAdapter.StockViewHolder>() {

    fun updateList(newItems: List<StockItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.admin_stock_overview, parent, false)
        return StockViewHolder(view)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class StockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        private val tvCrates: TextView = itemView.findViewById(R.id.tvCrates)
        private val tvPackets: TextView = itemView.findViewById(R.id.tvPackets)
        private val tvValue: TextView = itemView.findViewById(R.id.tvValue)

        fun bind(item: StockItem) {
            tvItemName.text = item.name
            
            val totalQty = item.stockQuantity
            // Avoid division by zero if pcsCount is null or 0
            val pcsCount = item.pcsCount?.takeIf { it > 0 } ?: 1
            
            val calculatedCrates = totalQty / pcsCount
            val remainingPackets = totalQty % pcsCount
            
            tvCrates.text = calculatedCrates.toString()
            tvPackets.text = remainingPackets.toString()
            
            // If API provides a valid stockValue, use it; otherwise calculate from quantity and price
            val calculatedValue = item.stockQuantity.toDouble() * item.sellingPrice
            val displayValue = if (item.stockValue != null && item.stockValue > 0) {
                item.stockValue
            } else {
                calculatedValue
            }

            tvValue.text = String.format(Locale.getDefault(), "₹%.2f", displayValue)
            
            // Toggle badge backgrounds
            tvCrates.setBackgroundResource(if (calculatedCrates > 0) R.drawable.bg_badge_blue else R.drawable.bg_badge_red)
            tvPackets.setBackgroundResource(if (remainingPackets > 0) R.drawable.bg_badge_teal else R.drawable.bg_badge_red)
        }
    }
}
