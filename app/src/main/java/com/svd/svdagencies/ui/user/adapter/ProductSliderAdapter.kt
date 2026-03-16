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

class ProductSliderAdapter(
    private val onProductClick: (AdminItem) -> Unit = {}
) : RecyclerView.Adapter<ProductSliderAdapter.ProductViewHolder>() {

    private var items: List<AdminItem> = emptyList()

    fun submitList(newItems: List<AdminItem>) {
        items = newItems
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

            itemView.setOnClickListener { onProductClick(item) }
        }
    }
}
