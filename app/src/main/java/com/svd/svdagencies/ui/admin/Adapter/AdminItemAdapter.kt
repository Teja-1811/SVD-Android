package com.svd.svdagencies.ui.admin.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
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
            tvBuyingPrice.text = "₹${item.buying_price}"
            tvSellingPrice.text = "₹${item.selling_price}"
            tvMrp.text = "₹${item.mrp}"
            tvMargin.text = "₹${item.margin}"
            tvStock.text = "${item.stock} in stock"

            // Load images logic here if using Glide/Picasso later
            // For now using placeholder

            btnEdit.setOnClickListener { onEditClick(item) }
            btnFreeze.setOnClickListener { onFreezeClick(item) }
        }
    }
}
