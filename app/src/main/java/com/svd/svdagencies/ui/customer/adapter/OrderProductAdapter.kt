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
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.customer.ProductResponse

class OrderProductAdapter(
    private val products: MutableList<ProductResponse>,
    private val cartQuantities: HashMap<Int, Double>,
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
            .inflate(R.layout.customer_order_product, parent, false)
        return ProductViewHolder(v)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {

        val product = products[position]
        val productId = product.id
        val step = product.calculateStep

        val qty = cartQuantities[productId] ?: 0.0
        val total = product.calculateTotal(qty)

        holder.tvName.text = "${product.name} (${product.company})"
        holder.tvPcs.text = "${product.pcs} pcs / crate"
        holder.tvMrp.text = "MRP: ₹${product.mrp}"
        holder.tvPrice.text = "₹${product.selling_price}"
        holder.tvMargin.text = "Margin: ${String.format("%.1f", product.margin)}%"
        holder.tvTotal.text = "₹${String.format("%.2f", total)}"

        holder.qtyWatcher?.let { holder.etQty.removeTextChangedListener(it) }
        holder.etQty.setText(product.formatQuantity(qty))

        val base = ApiClient.BASE_URL.removeSuffix("/")
        val productImg = product.image
        val url = if (productImg.startsWith("http")) {
            productImg
        } else {
            if (productImg.startsWith("/")) "$base$productImg" else "$base/$productImg"
        }

        Glide.with(holder.itemView.context)
            .load(url)
            .placeholder(R.drawable.ic_milk_placeholder)
            .error(R.drawable.ic_milk_placeholder)
            .into(holder.imgProduct)

        holder.btnPlus.setOnClickListener {
            val newQty = (cartQuantities[productId] ?: 0.0) + step
            cartQuantities[productId] = newQty
            notifyItemChanged(position)
            notifyGrandTotal()
        }

        holder.btnMinus.setOnClickListener {
            val newQty = (cartQuantities[productId] ?: 0.0) - step

            if (newQty <= 0) cartQuantities.remove(productId)
            else cartQuantities[productId] = newQty

            notifyItemChanged(position)
            notifyGrandTotal()
        }

        holder.qtyWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                var value = s?.toString()?.toDoubleOrNull() ?: 0.0
                value = (Math.round(value / step) * step)

                if (value <= 0) cartQuantities.remove(productId)
                else cartQuantities[productId] = value

                holder.tvTotal.text = "₹${String.format("%.2f", product.calculateTotal(value))}"
                notifyGrandTotal()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        holder.etQty.addTextChangedListener(holder.qtyWatcher)
    }

    override fun getItemCount(): Int = products.size

    private fun notifyGrandTotal() {
        var total = 0.0
        cartQuantities.forEach { (id, qty) ->
            val p = products.find { it.id == id } ?: return@forEach
            total += p.calculateTotal(qty)
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
