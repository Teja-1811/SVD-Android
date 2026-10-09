package com.svd.svdagencies.ui.admin.stock

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.admin.stock.StockCompanyTotal
import com.svd.svdagencies.data.model.admin.stock.StockDateEntry
import com.svd.svdagencies.data.model.admin.stock.StockLeakageEntry
import java.util.Locale

private fun Double.f2(): String = String.format(Locale.getDefault(), "%.2f", this)
private fun Double.rs2(): String = String.format(Locale.getDefault(), "Rs. %.2f", this)

private class InfoHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val title: TextView = itemView.findViewById(R.id.tvTitle)
    val subtitle: TextView = itemView.findViewById(R.id.tvSubtitle)
    val chip: TextView = itemView.findViewById(R.id.tvChip)
    val meta1Label: TextView = itemView.findViewById(R.id.tvMeta1Label)
    val meta1Value: TextView = itemView.findViewById(R.id.tvMeta1Value)
    val meta2Label: TextView = itemView.findViewById(R.id.tvMeta2Label)
    val meta2Value: TextView = itemView.findViewById(R.id.tvMeta2Value)
    val meta3Label: TextView = itemView.findViewById(R.id.tvMeta3Label)
    val meta3Value: TextView = itemView.findViewById(R.id.tvMeta3Value)
    
    val btnEdit: View? = itemView.findViewById(R.id.btnEdit)
    val btnDelete: View? = itemView.findViewById(R.id.btnDelete)
}

private fun ViewGroup.inflateInfo(): InfoHolder {
    val view = LayoutInflater.from(context).inflate(R.layout.admin_stock_info_item, this, false)
    return InfoHolder(view)
}

class StockDateEntryAdapter(
    private var entries: List<StockDateEntry> = emptyList(),
    private val onEdit: ((StockDateEntry) -> Unit)? = null,
    private val onDelete: ((StockDateEntry) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    fun updateList(newEntries: List<StockDateEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = parent.inflateInfo()
    override fun getItemCount(): Int = entries.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val h = holder as InfoHolder
        val entry = entries[position]
        h.title.text = entry.itemName
        h.subtitle.text = entry.companyName ?: "No company"
        h.chip.text = entry.createdAt?.takeLast(8)?.take(5) ?: "#${position + 1}"
        h.meta1Label.text = "Crates"
        h.meta1Value.text = entry.crates.f2()
        h.meta2Label.text = "Quantity"
        h.meta2Value.text = entry.quantity.f2()
        h.meta3Label.text = "Value"
        h.meta3Value.text = entry.value.rs2()
        
        h.btnEdit?.visibility = if (onEdit != null) View.VISIBLE else View.GONE
        h.btnDelete?.visibility = if (onDelete != null) View.VISIBLE else View.GONE
        
        h.btnEdit?.setOnClickListener { onEdit?.invoke(entry) }
        h.btnDelete?.setOnClickListener { onDelete?.invoke(entry) }
    }
}

class StockCompanyTotalAdapter(private var rows: List<StockCompanyTotal> = emptyList()) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    fun updateList(newRows: List<StockCompanyTotal>) {
        rows = newRows
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = parent.inflateInfo()
    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val h = holder as InfoHolder
        val row = rows[position]
        h.title.text = row.companyName
        h.subtitle.text = "Company total"
        h.chip.text = "#${position + 1}"
        h.meta1Label.text = "Total crates"
        h.meta1Value.text = row.totalCrates.f2()
        h.meta2Label.text = "Total quantity"
        h.meta2Value.text = row.totalQuantity.f2()
        h.meta3Label.text = "Total value"
        h.meta3Value.text = row.totalValue.rs2()
        
        h.btnEdit?.visibility = View.GONE
        h.btnDelete?.visibility = View.GONE
    }
}

class StockLeakageAdapter(
    private var entries: List<StockLeakageEntry> = emptyList(),
    private val onEdit: ((StockLeakageEntry) -> Unit)? = null,
    private val onDelete: ((StockLeakageEntry) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    fun updateList(newEntries: List<StockLeakageEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = parent.inflateInfo()
    override fun getItemCount(): Int = entries.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val h = holder as InfoHolder
        val entry = entries[position]
        h.title.text = entry.itemName ?: "Item"
        h.subtitle.text = entry.companyName ?: "No company"
        h.chip.text = entry.date
        h.meta1Label.text = "Leakage Qty"
        h.meta1Value.text = entry.quantity.toString()
        h.meta2Label.text = "Unit cost"
        h.meta2Value.text = String.format(Locale.getDefault(), "%.3f", entry.unitCost)
        h.meta3Label.text = "Loss"
        h.meta3Value.text = entry.totalLoss.rs2()
        
        h.btnEdit?.visibility = if (onEdit != null) View.VISIBLE else View.GONE
        h.btnDelete?.visibility = if (onDelete != null) View.VISIBLE else View.GONE
        
        h.btnEdit?.setOnClickListener { onEdit?.invoke(entry) }
        h.btnDelete?.setOnClickListener { onDelete?.invoke(entry) }
    }
}
