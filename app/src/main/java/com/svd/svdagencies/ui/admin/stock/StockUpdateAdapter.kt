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

class StockUpdateAdapter(private var items: List<StockItem>) :
    RecyclerView.Adapter<StockUpdateAdapter.UpdateViewHolder>() {

    private val updates = mutableMapOf<Int, Double>()

    fun updateList(newItems: List<StockItem>) {
        items = newItems
        updates.clear()
        notifyDataSetChanged()
    }

    fun getUpdates(): List<Map<String, Any>> {
        return updates.map { (id, crates) ->
            mapOf("id" to id, "crates" to crates)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpdateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.admin_stock_update_item, parent, false)
        return UpdateViewHolder(view)
    }

    override fun onBindViewHolder(holder: UpdateViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    inner class UpdateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        private val tvCurrentStock: TextView = itemView.findViewById(R.id.tvCurrentStock)
        private val etCrates: EditText = itemView.findViewById(R.id.etCrates)

        private var currentItem: StockItem? = null

        init {
            etCrates.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    currentItem?.let { item ->
                        val value = s.toString().toDoubleOrNull()
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
            tvCurrentStock.text = "Current: ${item.stockQuantity}"
            
            // Clear or set existing value if needed
            etCrates.setText(updates[item.id]?.toString() ?: "")
        }
    }
}
