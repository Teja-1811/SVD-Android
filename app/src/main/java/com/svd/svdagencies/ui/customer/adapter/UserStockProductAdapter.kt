package com.svd.svdagencies.ui.customer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.customer.ProductResponse
import java.text.NumberFormat
import java.util.Locale

class UserStockProductAdapter(
    private val quantities: Map<Int, Double>,
    private val onAdd: (ProductResponse) -> Unit
) : RecyclerView.Adapter<UserStockProductAdapter.Holder>() {
    private var products = emptyList<ProductResponse>()
    private val currency = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.stockProductImage)
        val name: TextView = view.findViewById(R.id.stockProductName)
        val price: TextView = view.findViewById(R.id.stockProductPrice)
        val quantity: TextView = view.findViewById(R.id.stockProductQuantity)
        val add: MaterialButton = view.findViewById(R.id.stockProductAdd)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.user_stock_product_card, parent, false))

    override fun getItemCount() = products.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val product = products[position]
        val count = quantities[product.id] ?: 0.0
        holder.name.text = product.name
        holder.price.text = currency.format(product.mrp)
        holder.quantity.text = if (count > 0) "${count.toInt()} in cart" else "In stock"
        holder.image.contentDescription = product.name
        holder.add.text = if (count >= product.stock) "In cart" else "Add to cart"
        holder.add.isEnabled = count < product.stock
        holder.add.contentDescription = "Add ${product.name} to cart"
        holder.add.setOnClickListener { onAdd(product) }
        Glide.with(holder.itemView)
            .load(ApiClient.getImageUrl(product.image))
            .override(256, 256)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.AUTOMATIC)
            .placeholder(R.drawable.ic_milk_placeholder)
            .error(R.drawable.ic_milk_placeholder)
            .fitCenter()
            .into(holder.image)
    }

    fun submitProducts(rows: List<ProductResponse>) {
        products = rows
        notifyDataSetChanged()
    }
}
