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
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.stock.StockItem

class DeliveryStockSingleInputAdapter(
    private val items: List<StockItem>
) : RecyclerView.Adapter<DeliveryStockSingleInputAdapter.ViewHolder>() {

    private val quantities = mutableMapOf<Int, Double>()

    init {
        items.forEach { item ->
            if (item.stockQuantity > 0) {
                quantities[item.id] = item.stockQuantity.toDouble()
            }
        }
    }

    fun getQuantities(): Map<Int, Double> = quantities

    fun clearQuantities() {
        quantities.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.delivery_stock_single_input, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, quantities[item.id] ?: 0.0) { qty ->
            quantities[item.id] = qty
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.tvItemName)
        private val tvCompany: TextView = view.findViewById(R.id.tvCompanyName)
        private val ivItem: ImageView = view.findViewById(R.id.ivItem)
        private val etCrateQty: EditText = view.findViewById(R.id.etCrateQty)
        private val etLooseQty: EditText = view.findViewById(R.id.etLooseQty)
        
        private var crateWatcher: TextWatcher? = null
        private var looseWatcher: TextWatcher? = null

        fun bind(item: StockItem, currentTotalQty: Double, onUpdate: (Double) -> Unit) {
            tvName.text = item.name
            tvCompany.text = item.companyName ?: ""

            val mainUrl: String?
            val fallbackUrl: String?

            if (!item.image.isNullOrEmpty()) {
                mainUrl = ApiClient.getImageUrl(item.image)
                fallbackUrl = null
            } else if (!item.itemCode.isNullOrEmpty()) {
                mainUrl = ApiClient.getImageUrl("${item.itemCode}.png")
                fallbackUrl = ApiClient.getImageUrl("${item.itemCode}.jpg")
            } else {
                mainUrl = null
                fallbackUrl = null
            }

            Glide.with(itemView.context)
                .load(mainUrl)
                .placeholder(R.drawable.ic_milk_placeholder)
                .error(
                    Glide.with(itemView.context)
                        .load(fallbackUrl)
                        .error(R.drawable.ic_milk_placeholder)
                )
                .into(ivItem)
            
            val pcs = item.pcsCount ?: 1
            val pcsCount = if (pcs > 0) pcs else 1
            
            val crates = (currentTotalQty / pcsCount).toInt()
            val loose = (currentTotalQty % pcsCount).toInt()

            etCrateQty.removeTextChangedListener(crateWatcher)
            etLooseQty.removeTextChangedListener(looseWatcher)

            etCrateQty.setText(if (crates > 0) crates.toString() else "")
            etLooseQty.setText(if (loose > 0) loose.toString() else "")
            
            val updateAction = {
                val c = etCrateQty.text.toString().toIntOrNull() ?: 0
                val l = etLooseQty.text.toString().toIntOrNull() ?: 0
                val total = (c * pcsCount + l).toDouble()
                onUpdate(total)
            }

            crateWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) { updateAction() }
            }

            looseWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) { updateAction() }
            }

            etCrateQty.addTextChangedListener(crateWatcher)
            etLooseQty.addTextChangedListener(looseWatcher)
        }
    }
}
