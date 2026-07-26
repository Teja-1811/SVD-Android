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
        private val tvCurrentStock: TextView = itemView.findViewById(R.id.tvCurrentStock)
        private val tvBuyingPrice: TextView = itemView.findViewById(R.id.tvBuyingPrice)
        private val tvValue: TextView = itemView.findViewById(R.id.tvValue)

        fun bind(item: StockItem) {
            tvItemName.text = item.name
            tvCurrentStock.text = item.stockQuantity.toString()
            tvBuyingPrice.text = String.format(Locale.getDefault(), "%.3f", item.buyingPrice)

            val calculatedValue = item.stockQuantity.toDouble() * item.buyingPrice
            val displayValue = item.stockValue?.takeIf { it > 0 } ?: calculatedValue
            tvValue.text = String.format(Locale.getDefault(), "Rs. %.2f", displayValue)
            tvCurrentStock.setBackgroundResource(
                if (item.stockQuantity > 5) R.drawable.bg_badge_blue else R.drawable.bg_badge_red
            )
        }
    }
}
