package com.svd.svdagencies.ui.delivery.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.admin.CatalogItem
import com.svd.svdagencies.data.model.delivery.StockEntryInput

class DeliveryStockInputAdapter(
    private val items: List<CatalogItem>
) : RecyclerView.Adapter<DeliveryStockInputAdapter.ViewHolder>() {

    private val inputs = items.map { StockEntryInput(itemId = it.id) }

    fun getEntries(): List<StockEntryInput> = inputs

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_delivery_stock_input, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val input = inputs[position]
        holder.bind(item, input)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.tvItemName)
        private val tvCompany: TextView = view.findViewById(R.id.tvCompanyName)
        private val etMngCol: EditText = view.findViewById(R.id.etMorningCollected)
        private val etMngRet: EditText = view.findViewById(R.id.etMorningReturned)
        private val etEveCol: EditText = view.findViewById(R.id.etEveningCollected)
        private val etEveRet: EditText = view.findViewById(R.id.etEveningReturned)

        fun bind(item: CatalogItem, input: StockEntryInput) {
            tvName.text = item.name
            tvCompany.text = item.company_name

            setupWatcher(etMngCol) { input.morningCollected = it }
            setupWatcher(etMngRet) { input.morningReturned = it }
            setupWatcher(etEveCol) { input.eveningCollected = it }
            setupWatcher(etEveRet) { input.eveningReturned = it }
        }

        private fun setupWatcher(editText: EditText, onUpdate: (Double) -> Unit) {
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    onUpdate(s.toString().toDoubleOrNull() ?: 0.0)
                }
            })
        }
    }
}
