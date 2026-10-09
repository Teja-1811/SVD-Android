package com.svd.svdagencies.ui.delivery

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.delivery.DeliveryBillItem

class DeliveryBillSelectAdapter(
    private var items: List<DeliveryBillItem> = emptyList(),
    private val onQtyChanged: (DeliveryBillItem, Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val itemQuantities = mutableMapOf<Int, Int>()
    private var userType: String = "user"

    fun setUserType(type: String) {
        userType = type
        notifyDataSetChanged()
    }

    fun submitList(newList: List<DeliveryBillItem>) {
        items = newList
        resetQuantities()
    }

    fun submitListPreservingQuantities(newList: List<DeliveryBillItem>) {
        val existingQuantities = itemQuantities.toMap()
        items = newList
        itemQuantities.clear()
        items.forEach { item ->
            itemQuantities[item.itemId] = existingQuantities[item.itemId]?.coerceAtMost(item.stockQuantity) ?: 0
        }
        notifyDataSetChanged()
    }

    fun resetQuantities() {
        itemQuantities.clear()
        items.forEach { itemQuantities[it.itemId] = 0 }
        notifyDataSetChanged()
    }

    fun setInitialQuantities(qtys: Map<Int, Int>) {
        qtys.forEach { (itemId, qty) ->
            val item = items.find { it.itemId == itemId }
            if (item != null) {
                // Ensure we don't exceed stock
                itemQuantities[itemId] = if (qty > item.stockQuantity) item.stockQuantity else qty
            }
        }
        notifyDataSetChanged()
    }

    fun applyStockDeductions(soldItems: List<Pair<DeliveryBillItem, Int>>) {
        val soldByItemId = soldItems
            .groupBy({ it.first.itemId }, { it.second })
            .mapValues { entry -> entry.value.sum() }

        items = items.map { item ->
            val soldQty = soldByItemId[item.itemId] ?: 0
            if (soldQty > 0) {
                item.copy(stockQuantity = (item.stockQuantity - soldQty).coerceAtLeast(0))
            } else {
                item
            }
        }
        resetQuantities()
    }

    fun getSelectedItemsWithQty(): List<Pair<DeliveryBillItem, Int>> {
        return items.filter { (itemQuantities[it.itemId] ?: 0) > 0 }
            .map { it to (itemQuantities[it.itemId] ?: 0) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(inflater.inflate(R.layout.delivery_bill_item_select, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder).bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName = itemView.findViewById<TextView>(R.id.tvItemName)
        private val tvPrice = itemView.findViewById<TextView>(R.id.tvPrice)
        private val tvStock = itemView.findViewById<TextView>(R.id.tvStock)
        private val tvBadge = itemView.findViewById<TextView>(R.id.tvBadge)
        private val ivImage = itemView.findViewById<ImageView>(R.id.ivItemImage)
        private val etQty = itemView.findViewById<EditText>(R.id.etQty)
        private val btnDecrease = itemView.findViewById<MaterialButton>(R.id.btnDecrease)
        private val btnIncrease = itemView.findViewById<MaterialButton>(R.id.btnIncrease)
        private var currentItem: DeliveryBillItem? = null

        private val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val item = currentItem ?: return
                var newQty = s.toString().toIntOrNull() ?: 0
                if (newQty > item.stockQuantity) {
                    newQty = item.stockQuantity
                    etQty.removeTextChangedListener(this)
                    etQty.setText(newQty.toString())
                    etQty.setSelection(etQty.text.length)
                    etQty.addTextChangedListener(this)
                    Toast.makeText(
                        etQty.context,
                        "Only ${item.stockQuantity} unit(s) available for ${item.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                if (itemQuantities[item.itemId] != newQty) {
                    itemQuantities[item.itemId] = newQty
                    onQtyChanged(item, newQty)
                }
            }
        }

        init {
            etQty.addTextChangedListener(textWatcher)
            etQty.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    etQty.clearFocus()
                    true
                } else {
                    false
                }
            }
        }

        fun bind(item: DeliveryBillItem) {
            currentItem = item
            tvName.text = item.name
            tvStock.text = if (item.stockQuantity > 0) {
                "Stock: ${item.stockQuantity}"
            } else {
                "Out of stock"
            }
            tvBadge.text = item.stockQuantity.toString()
            tvBadge.setBackgroundResource(
                if (item.stockQuantity > 0) R.drawable.bg_badge_green else R.drawable.bg_badge_red
            )
            
            // `price` is the final customer price resolved by the server.
            val displayPrice = item.price
            tvPrice.text = "₹%.2f".format(displayPrice)
            
            // Temporary remove listener to avoid trigger during bind
            etQty.removeTextChangedListener(textWatcher)
            val currentQty = itemQuantities[item.itemId] ?: 0
            etQty.setText(currentQty.toString())
            etQty.addTextChangedListener(textWatcher)
            etQty.isEnabled = item.stockQuantity > 0
            btnIncrease.isEnabled = item.stockQuantity > 0
            btnIncrease.alpha = if (item.stockQuantity > 0) 1f else 0.45f

            val imageUrl = ApiClient.getImageUrl(item.imageUrl)

            Glide.with(ivImage.context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_milk_placeholder)
                .error(R.drawable.ic_milk_placeholder)
                .into(ivImage)

            btnDecrease.setOnClickListener {
                val qty = itemQuantities[item.itemId] ?: 0
                if (qty > 0) {
                    val newQty = qty - 1
                    itemQuantities[item.itemId] = newQty
                    etQty.setText(newQty.toString())
                    onQtyChanged(item, newQty)
                }
            }

            btnIncrease.setOnClickListener {
                val qty = itemQuantities[item.itemId] ?: 0
                val newQty = qty + 1
                if (newQty > item.stockQuantity) {
                    Toast.makeText(
                        itemView.context,
                        "Only ${item.stockQuantity} unit(s) available for ${item.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                itemQuantities[item.itemId] = newQty
                etQty.setText(newQty.toString())
                onQtyChanged(item, newQty)
            }
        }
    }

}
