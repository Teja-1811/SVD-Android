package com.svd.svdagencies.ui.customer.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.customer.ProductResponse

class OrderProductAdapter(
    private val products: MutableList<ProductResponse>,
    private val cartQuantities: HashMap<Int, Double>,   // 🔹 Double because 0.5 allowed
    private val onTotalChanged: (Double) -> Unit
) : RecyclerView.Adapter<OrderProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val imgProduct: ImageView = view.findViewById(R.id.imgProduct)
        val tvName: TextView = view.findViewById(R.id.tvName)

        val tvPcs: TextView = view.findViewById(R.id.tvPcs)
        val tvMrp: TextView = view.findViewById(R.id.tvMrp)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvMargin: TextView = view.findViewById(R.id.tvMargin)

        val etQty: EditText = view.findViewById(R.id.etQty)
        val tvTotal: TextView = view.findViewById(R.id.tvTotal)

        val btnPlus: ImageButton = view.findViewById(R.id.btnPlus)
        val btnMinus: ImageButton = view.findViewById(R.id.btnMinus)

        var qtyWatcher: TextWatcher? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_product, parent, false)
        return ProductViewHolder(v)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {

        val product = products[position]
        val productId = product.id

        // 🔹 RULE: if pcs > 1 → step 0.5 else 1
        val pcs = product.pcs_count ?: 1

        val step = when {
            pcs == 1 -> 1.0
            pcs == 5 -> 0.2
            pcs > 10 -> 0.5
            else -> 1.0
        }


        val qty = cartQuantities[productId] ?: 0.0   // quantity in crates/units
        val total = qty * pcs * product.selling_price

        holder.tvName.text = product.name
        holder.tvPcs.text = "$pcs pcs / crate"
        holder.tvMrp.text = "MRP: ₹${product.mrp}"
        holder.tvPrice.text = "₹${product.selling_price}"
        holder.tvMargin.text = "Margin: ${String.format("%.1f", product.margin)}%"
        holder.tvTotal.text = "₹${String.format("%.2f", total)}"

        // Show nice qty text
        holder.qtyWatcher?.let { holder.etQty.removeTextChangedListener(it) }
        holder.etQty.setText(
            if (qty == qty.toInt().toDouble()) qty.toInt().toString()
            else qty.toString()
        )

        // ---------- IMAGE ----------
        val base = "http://ec2-18-235-222-205.compute-1.amazonaws.com"
        val url = if (product.image.startsWith("http")) product.image else base + product.image

        Glide.with(holder.itemView.context)
            .load(url)
            .placeholder(R.drawable.ic_milk_placeholder)
            .error(R.drawable.ic_milk_placeholder)
            .into(holder.imgProduct)

        // ---------- PLUS ----------
        holder.btnPlus.setOnClickListener {
            val newQty = (cartQuantities[productId] ?: 0.0) + step
            cartQuantities[productId] = newQty
            notifyItemChanged(position)
            notifyGrandTotal()
        }

        // ---------- MINUS ----------
        holder.btnMinus.setOnClickListener {
            val newQty = (cartQuantities[productId] ?: 0.0) - step

            if (newQty <= 0) cartQuantities.remove(productId)
            else cartQuantities[productId] = newQty

            notifyItemChanged(position)
            notifyGrandTotal()
        }

        // ---------- MANUAL INPUT ----------
        holder.qtyWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

                var value = s?.toString()?.toDoubleOrNull() ?: 0.0

                // snap to nearest step
                value = (Math.round(value / step) * step)

                if (value <= 0) cartQuantities.remove(productId)
                else cartQuantities[productId] = value

                val totalValue = value * pcs * product.selling_price
                holder.tvTotal.text = "₹${String.format("%.2f", totalValue)}"

                notifyGrandTotal()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        holder.etQty.addTextChangedListener(holder.qtyWatcher)
    }

    override fun getItemCount(): Int = products.size

    // ---------- GRAND TOTAL ----------
    private fun notifyGrandTotal() {
        var total = 0.0
        cartQuantities.forEach { (id, qty) ->
            val p = products.find { it.id == id } ?: return@forEach
            val pcs = p.pcs_count ?: 1
            total += qty * pcs * p.selling_price
        }
        onTotalChanged(total)
    }

    fun updateProducts(newProducts: List<ProductResponse>) {
        products.clear()
        products.addAll(newProducts)
        notifyDataSetChanged()
        notifyGrandTotal()
    }

    fun getCartItems(): HashMap<Int, Double> = cartQuantities
}
