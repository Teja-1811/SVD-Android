package com.svd.svdagencies.ui.admin.stock

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.admin.stock.StockItem
import com.svd.svdagencies.data.model.admin.stock.StockCompany

class StockUpdateAdapter(private var rawItems: List<StockItem> = emptyList()) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_HEADER = 0
    private val TYPE_ITEM = 1
    
    private var displayItems = mutableListOf<Any>()
    private val updates = mutableMapOf<Int, StockDraft>()
    private var companies: List<StockCompany> = emptyList()
    private var searchTerm: String = ""
    private var companyFilter: String = "All companies"

    fun updateList(newItems: List<StockItem>) {
        rawItems = newItems
        rebuildDisplayItems()
    }

    fun updateCompanies(newCompanies: List<StockCompany>) {
        companies = newCompanies
    }

    fun filter(search: String, company: String) {
        searchTerm = search.trim()
        companyFilter = company.ifBlank { "All companies" }
        rebuildDisplayItems()
    }

    private fun rebuildDisplayItems() {
        val search = searchTerm.lowercase()
        val filtered = rawItems.filter { item ->
            val companyName = item.companyName.orEmpty()
            val matchesSearch = search.isEmpty() ||
                item.name.lowercase().contains(search) ||
                companyName.lowercase().contains(search)
            val matchesCompany = companyFilter == "All companies" || companyName == companyFilter
            matchesSearch && matchesCompany
        }
        val grouped = filtered.groupBy { it.categoryName?.ifBlank { null } ?: "General" }

        displayItems.clear()
        for ((category, items) in grouped) {
            displayItems.add(CategoryHeader(category, items.size))
            displayItems.addAll(items)
        }

        notifyDataSetChanged()
    }

    data class CategoryHeader(val name: String, val count: Int)
    data class StockDraft(
        val crates: Double,
        val discount: Double,
        val companyId: Int,
        val companyName: String,
    )

    fun getUpdates(): List<Map<String, Any>> {
        return updates.map { (id, draft) ->
            mapOf(
                "id" to id,
                "company_id" to draft.companyId,
                "crates" to draft.crates,
                "discount" to draft.discount,
            )
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
        private val spinnerCompany: Spinner = itemView.findViewById(R.id.spinnerCompany)
        private val tvCurrentStock: TextView = itemView.findViewById(R.id.tvCurrentStock)
        private val tvPcsCount: TextView = itemView.findViewById(R.id.tvPcsCount)
        private val tvBuyingPrice: TextView = itemView.findViewById(R.id.tvBuyingPrice)
        private val tvQuantityPreview: TextView = itemView.findViewById(R.id.tvQuantityPreview)
        private val tvValuePreview: TextView = itemView.findViewById(R.id.tvValuePreview)
        private val etCrates: EditText = itemView.findViewById(R.id.etCrates)
        private val etDiscount: EditText = itemView.findViewById(R.id.etDiscount)

        private var currentItem: StockItem? = null
        private var isBinding = false

        init {
            etCrates.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    currentItem?.let { item ->
                        updateDraft(item)
                    }
                }
            })
            etDiscount.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    currentItem?.let { item ->
                        updateDraft(item)
                    }
                }
            })
            spinnerCompany.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (!isBinding) {
                        currentItem?.let(::updateDraft)
                    }
                }
            }
        }

        fun bind(item: StockItem) {
            isBinding = true
            currentItem = item
            tvItemName.text = item.name
            tvCurrentStock.text = item.stockQuantity.toString()
            tvPcsCount.text = "Pcs / crate: ${item.pcsCount ?: 1}"
            tvBuyingPrice.text = "Buying: %.3f".format(item.buyingPrice)

            val draft = updates[item.id]
            etCrates.setText(draft?.crates?.toCleanString() ?: "0")
            etDiscount.setText(draft?.discount?.toCleanString() ?: "0")
            val supplierAdapter = ArrayAdapter(itemView.context, android.R.layout.simple_spinner_dropdown_item, companies.map { it.name })
            spinnerCompany.adapter = supplierAdapter
            val selectedCompanyId = draft?.companyId ?: item.companyId
            val selectedIndex = companies.indexOfFirst { it.id == selectedCompanyId }.let { index ->
                if (index >= 0) index else 0
            }
            if (companies.isNotEmpty()) spinnerCompany.setSelection(selectedIndex)
            isBinding = false
            refreshPreview(item)
        }

        private fun updateDraft(item: StockItem) {
            val crates = etCrates.text.toString().toDoubleOrNull() ?: 0.0
            val discount = etDiscount.text.toString().toDoubleOrNull() ?: 0.0
            val company = companies.getOrNull(spinnerCompany.selectedItemPosition)
            if (crates > 0 && company != null) {
                updates[item.id] = StockDraft(
                    crates,
                    discount.coerceAtLeast(0.0),
                    company.id,
                    company.name,
                )
            } else {
                updates.remove(item.id)
            }
            refreshPreview(item)
        }

        private fun refreshPreview(item: StockItem) {
            val crates = etCrates.text.toString().toDoubleOrNull() ?: 0.0
            val discount = etDiscount.text.toString().toDoubleOrNull() ?: 0.0
            val quantity = crates * (item.pcsCount ?: 1)
            val supplier = companies.getOrNull(spinnerCompany.selectedItemPosition)
            val isDodla = (supplier?.name ?: "").equals("Dodla", ignoreCase = true)
            val value = if (isDodla) {
                val invoiceQuantity = crates * item.litresPerCrate
                val taxableValue = (invoiceQuantity * item.basePrice) * (1 - (discount / 100.0))
                taxableValue + (taxableValue * item.gstPercentage / 100.0)
            } else {
                (quantity * item.buyingPrice) - discount
            }.coerceAtLeast(0.0)
            tvQuantityPreview.text = "Quantity: %.3f".format(quantity)
            tvValuePreview.text = "Value: %.3f".format(value)
        }

        private fun Double.toCleanString(): String {
            return if (this % 1.0 == 0.0) this.toInt().toString() else this.toString()
        }
    }
}
