package com.svd.svdagencies.ui.user.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.Items.AdminItem
import com.google.android.material.button.MaterialButton

class ProductSliderAdapter(
    private val onAddToCart: (AdminItem) -> Unit = {},
    private val onIncrease: (AdminItem) -> Unit = {},
    private val onDecrease: (AdminItem) -> Unit = {},
    private val quantityProvider: (AdminItem) -> Int = { 0 },
    private val onProductClick: (AdminItem) -> Unit = {}
) : RecyclerView.Adapter<ProductSliderAdapter.ProductViewHolder>() {

    private var items: List<AdminItem> = emptyList()
    private var allowOutOfStock: Boolean = false

    fun submitList(newItems: List<AdminItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setAllowOutOfStock(allow: Boolean) {
        allowOutOfStock = allow
        notifyDataSetChanged()
    }

    fun refreshQuantities() {
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_product_card, parent, false)
        return ProductViewHolder(view, onProductClick)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ProductViewHolder(
        itemView: View,
        private val onProductClick: (AdminItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val imgProduct: ImageView = itemView.findViewById(R.id.imgProduct)
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvProductPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
        private val tvStockInfo: TextView = itemView.findViewById(R.id.tvStockInfo)
        private val tvStockBadge: TextView = itemView.findViewById(R.id.tvStockBadge)
        private val btnAddToCart: MaterialButton = itemView.findViewById(R.id.btnAddToCart)
        private val btnIncrease: MaterialButton = itemView.findViewById(R.id.btnIncrease)
        private val btnDecrease: MaterialButton = itemView.findViewById(R.id.btnDecrease)
        private val tvQty: TextView = itemView.findViewById(R.id.tvQty)
        private val layoutQty: View = itemView.findViewById(R.id.layoutQtyControls)

        fun bind(item: AdminItem) {
            tvProductName.text = item.name
            tvProductPrice.text = "\u20B9${item.mrp ?: "--"}"

            val imageUrl = item.image?.let {
                if (it.startsWith("http", ignoreCase = true)) it else "${ApiClient.BASE_URL.removeSuffix("/")}/${it.removePrefix("/")}"
            } ?: ""

            Glide.with(itemView)
                .load(imageUrl)
                .placeholder(R.drawable.ic_milk_placeholder)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imgProduct)

            val available = item.stock_quantity ?: 0
            val inStock = allowOutOfStock || available > 1

            if (inStock) {
                tvStockBadge.visibility = View.GONE
                itemView.alpha = 1f
                tvStockInfo.text = "In stock"
            } else {
                tvStockBadge.visibility = View.VISIBLE
                tvStockBadge.text = "Out of stock"
                itemView.alpha = 1f
                tvStockInfo.text = "Out of stock"
            }

            btnAddToCart.isEnabled = inStock
            btnAddToCart.alpha = if (inStock) 1f else 0.8f
            btnAddToCart.setOnClickListener {
                if (inStock) onAddToCart(item)
            }

            val qty = quantityProvider(item)
            if (qty > 0) {
                btnAddToCart.visibility = View.GONE
                layoutQty.visibility = View.VISIBLE
                tvQty.text = qty.toString()
                btnIncrease.isEnabled = inStock
                btnDecrease.isEnabled = true
                btnIncrease.setOnClickListener { if (inStock) onIncrease(item) }
                btnDecrease.setOnClickListener { onDecrease(item) }
            } else {
                btnAddToCart.visibility = View.VISIBLE
                layoutQty.visibility = View.GONE
            }

            itemView.setOnClickListener { onProductClick(item) }
        }
    }
}
