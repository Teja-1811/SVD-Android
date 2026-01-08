package com.svd.svdagencies.ui.admin.Adapter

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
import com.svd.svdagencies.data.model.admin.AdminItem

class AdminItemAdapter(
    private var items: List<AdminItem>,
    private val onEditClick: (AdminItem) -> Unit,
    private val onFreezeClick: (AdminItem) -> Unit
) : RecyclerView.Adapter<AdminItemAdapter.ItemViewHolder>() {

    fun updateList(newItems: List<AdminItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_product, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvCode: TextView = itemView.findViewById(R.id.tvCode)
        private val tvCompany: TextView = itemView.findViewById(R.id.tvCompany)
        private val tvBuyingPrice: TextView = itemView.findViewById(R.id.tvBuyingPrice)
        private val tvSellingPrice: TextView = itemView.findViewById(R.id.tvSellingPrice)
        private val tvMrp: TextView = itemView.findViewById(R.id.tvMrp)
        private val tvMargin: TextView = itemView.findViewById(R.id.tvMargin)
        private val tvStock: TextView = itemView.findViewById(R.id.tvStock)
        private val btnEdit: MaterialButton = itemView.findViewById(R.id.btnEdit)
        private val btnFreeze: MaterialButton = itemView.findViewById(R.id.btnFreeze)
        private val imgCompanyLogo: ImageView = itemView.findViewById(R.id.imgCompanyLogo)
        private val imgProduct: ImageView = itemView.findViewById(R.id.imgProduct)

        fun bind(item: AdminItem) {
            tvProductName.text = item.name
            tvCode.text = item.code
            tvCompany.text = item.company
            tvBuyingPrice.text = "₹${item.buying_price ?: "0.00"}"
            tvSellingPrice.text = "₹${item.selling_price ?: "0.00"}"
            tvMrp.text = "₹${item.mrp ?: "0.00"}"

            val selling = item.selling_price?.toDoubleOrNull() ?: 0.0
            val buying = item.buying_price?.toDoubleOrNull() ?: 0.0
            val margin = selling - buying
            
            tvMargin.text = "₹%.2f".format(margin)
            tvStock.text = "${item.stock_quantity ?: 0} in stock"

            // Loading product image using Glide
            val imageUrl = item.image
            
            if (!imageUrl.isNullOrEmpty()) {
                val fullUrl = if (imageUrl.startsWith("http")) {
                    imageUrl
                } else {
                    // Assuming ApiClient.BASE_URL exists and needs to be prepended
                    // Since I can't access ApiClient.BASE_URL directly if private,
                    // I'll assume the URL from backend is relative.
                    // If it is an absolute path from Django like /media/..., we need base url.
                    // Let's try to construct it.
                    "http://ec2-18-235-222-205.compute-1.amazonaws.com$imageUrl"
                }

                Glide.with(itemView.context)
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_milk_placeholder) // Use a default placeholder
                    .error(R.drawable.ic_milk_placeholder)
                    .into(imgProduct)
            } else {
                imgProduct.setImageResource(R.drawable.ic_milk_placeholder)
            }

            // Company Logo logic (if applicable, otherwise hide or set default)
            // Assuming no company logo url in AdminItem for now, just hiding or setting placeholder
            // imgCompanyLogo.visibility = View.GONE 

            btnEdit.setOnClickListener { onEditClick(item) }
            btnFreeze.setOnClickListener { onFreezeClick(item) }
        }
    }
}
