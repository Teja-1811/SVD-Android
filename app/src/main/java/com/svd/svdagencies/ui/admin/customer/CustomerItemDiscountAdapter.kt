package com.svd.svdagencies.ui.admin.customer

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.admin.customerData.CustomerItemDiscountRow
import java.util.Locale

class CustomerItemDiscountAdapter : RecyclerView.Adapter<CustomerItemDiscountAdapter.Holder>() {
    private val items = mutableListOf<CustomerItemDiscountRow>()
    private var usesMrp = false

    fun submitList(rows: List<CustomerItemDiscountRow>) {
        items.clear()
        items.addAll(rows)
        notifyDataSetChanged()
    }

    fun values() = items.map { it.copy() }

    fun setUsesMrp(value: Boolean) {
        if (usesMrp == value) return
        usesMrp = value
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.admin_customer_item_discount_row, parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position], usesMrp)
    override fun getItemCount() = items.size

    class Holder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.tvItemName)
        private val price: TextView = view.findViewById(R.id.tvPrice)
        private val discount: TextInputEditText = view.findViewById(R.id.etDiscount)
        private val finalPrice: TextView = view.findViewById(R.id.tvFinalPrice)
        private var watcher: TextWatcher? = null

        fun bind(item: CustomerItemDiscountRow, usesMrp: Boolean) {
            name.text = item.name
            val basePrice = if (usesMrp) item.mrp else item.selling_price
            val priceLabel = if (usesMrp) "MRP" else "Price"
            price.text = "${item.code.takeIf { it.isNotBlank() }?.plus("  •  ") ?: ""}$priceLabel: ${money(basePrice)}"
            watcher?.let { discount.removeTextChangedListener(it) }
            discount.setText(if (item.discount_per_unit == 0.0) "" else "%.2f".format(Locale.US, item.discount_per_unit))
            showFinal(basePrice, item.discount_per_unit)
            watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    item.discount_per_unit = s.toString().toDoubleOrNull() ?: 0.0
                    showFinal(basePrice, item.discount_per_unit)
                }
            }
            discount.addTextChangedListener(watcher)
        }

        private fun showFinal(basePrice: Double, amount: Double) {
            finalPrice.text = "Final: ${money(basePrice - amount)}"
        }

        private fun money(value: Double) = String.format(Locale.getDefault(), "₹%.2f", value)
    }
}
