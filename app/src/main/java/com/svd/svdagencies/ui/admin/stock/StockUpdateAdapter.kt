package com.svd.svdagencies.ui.admin.stock

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.admin.stock.StockItem

class StockUpdateAdapter(private var rawItems: List<StockItem> = emptyList()) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_HEADER = 0
    private val TYPE_ITEM = 1
    
    private var displayItems = mutableListOf<Any>()
    private val updates = mutableMapOf<Int, Int>()

    fun updateList(newItems: List<StockItem>) {
        rawItems = newItems
        val grouped = newItems.groupBy { it.categoryName ?: "General" }
        
        displayItems.clear()
        for ((category, items) in grouped) {
            displayItems.add(CategoryHeader(category, items.size))
            displayItems.addAll(items)
        }
        
        updates.clear()
        notifyDataSetChanged()
    }

    data class CategoryHeader(val name: String, val count: Int)

    fun getUpdates(): List<Map<String, Any>> {
        return updates.map { (id, quantity) ->
            mapOf("id" to id, "crates" to quantity)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (displayItems[position] is CategoryHeader) TYPE_HEADER else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.admin_stock_item_category_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.admin_stock_update_item, parent, false)
            UpdateViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            holder.bind(displayItems[position] as CategoryHeader)
        } else if (holder is UpdateViewHolder) {
            holder.bind(displayItems[position] as StockItem)
        }
    }

    override fun getItemCount(): Int = displayItems.size

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvHeader: TextView = itemView.findViewById(R.id.tvCategoryHeader)
        private val tvCount: TextView = itemView.findViewById(R.id.tvItemCount)
        
        fun bind(header: CategoryHeader) {
            tvHeader.text = header.name
            tvCount.text = "${header.count} items"
        }
    }

    inner class UpdateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        private val tvCompanyName: TextView = itemView.findViewById(R.id.tvCompanyName)
        private val tvCurrentStock: TextView = itemView.findViewById(R.id.tvCurrentStock)
        private val etCrates: EditText = itemView.findViewById(R.id.etCrates)

        private var currentItem: StockItem? = null

        init {
            etCrates.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    currentItem?.let { item ->
                        val value = s.toString().toIntOrNull()
                        if (value != null && value > 0) {
                            updates[item.id] = value
                        } else {
                            updates.remove(item.id)
                        }
                    }
                }
            })
        }

        fun bind(item: StockItem) {
            currentItem = item
            tvItemName.text = item.name
            tvCompanyName.text = item.companyName
            tvCurrentStock.text = item.stockQuantity.toString()
            
            // Set existing update value or default to 0
            val updateVal = updates[item.id]?.toString() ?: "0"
            etCrates.setText(updateVal)
        }
    }
}
