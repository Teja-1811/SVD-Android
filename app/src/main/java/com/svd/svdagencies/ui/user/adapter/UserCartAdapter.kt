package com.svd.svdagencies.ui.user.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.ui.user.model.CartItem

class UserCartAdapter(
    private val onQuantityChanged: (itemId: Int, quantity: Int) -> Unit,
    private val onRemove: (itemId: Int) -> Unit
) : RecyclerView.Adapter<UserCartAdapter.CartViewHolder>() {

    private var items: List<CartItem> = emptyList()
    private var allowOutOfStock: Boolean = false

    fun submitList(newItems: List<CartItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setAllowOutOfStock(allow: Boolean) {
        allowOutOfStock = allow
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_cart_item, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgProduct: ImageView = itemView.findViewById(R.id.imgProduct)
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val tvStock: TextView = itemView.findViewById(R.id.tvStock)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        private val btnDecrease: MaterialButton = itemView.findViewById(R.id.btnDecrease)
        private val btnIncrease: MaterialButton = itemView.findViewById(R.id.btnIncrease)
        private val btnRemove: TextView = itemView.findViewById(R.id.btnRemove)

        fun bind(cartItem: CartItem) {
            val item = cartItem.item
            val available = item.stock_quantity ?: 0
            val unitPrice = if (item.sellingPriceValue > 0) item.sellingPriceValue else item.mrpValue

            tvProductName.text = item.name
            tvPrice.text = "\u20B9${String.format("%.2f", unitPrice)}"
            tvQuantity.text = cartItem.quantity.toString()

            val inStock = allowOutOfStock || available > 1
            if (inStock) {
                tvStock.visibility = View.GONE
                itemView.alpha = 1f
            } else {
                tvStock.visibility = View.VISIBLE
                tvStock.text = "OUT OF STOCK"
                tvStock.setBackgroundResource(R.drawable.bg_status_red)
                itemView.alpha = 0.6f
            }

            btnIncrease.isEnabled = inStock && cartItem.quantity < available
            btnDecrease.isEnabled = cartItem.quantity > 0

            val imageUrl = item.image?.let {
                if (it.startsWith("http", ignoreCase = true)) it else "${ApiClient.BASE_URL.removeSuffix("/")}/${it.removePrefix("/")}"
            } ?: ""

            Glide.with(itemView)
                .load(imageUrl)
                .placeholder(R.drawable.ic_milk_placeholder)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imgProduct)

            btnIncrease.setOnClickListener {
                val newQty = cartItem.quantity + 1
                if (newQty <= available) onQuantityChanged(item.id, newQty)
            }

            btnDecrease.setOnClickListener {
                val newQty = cartItem.quantity - 1
                if (newQty <= 0) onRemove(item.id) else onQuantityChanged(item.id, newQty)
            }

            btnRemove.setOnClickListener { onRemove(item.id) }
        }
    }
}
