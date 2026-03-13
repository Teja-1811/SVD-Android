package com.svd.svdagencies.ui.customer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.data.model.customer.ProductData
import com.svd.svdagencies.databinding.CustomerCatalogItemCardBinding
import java.text.NumberFormat
import java.util.Locale

class CustomerCatalogAdapter : ListAdapter<ProductData, CustomerCatalogAdapter.CatalogViewHolder>(CatalogDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatalogViewHolder {
        val binding = CustomerCatalogItemCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CatalogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CatalogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CatalogViewHolder(private val binding: CustomerCatalogItemCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProductData) {
            binding.tvProductName.text = item.name
            
            val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            
            // Set Selling Price
            binding.tvPrice.text = formatter.format(item.sellingPrice)
            
            // Set MRP
            binding.tvMrp.text = formatter.format(item.mrp)
            
            // Set Margin (MRP - Selling Price)
            val marginValue = item.mrp - item.sellingPrice
            binding.tvMargin.text = "Margin: ${formatter.format(marginValue)}"
        }
    }

    class CatalogDiffCallback : DiffUtil.ItemCallback<ProductData>() {
        override fun areItemsTheSame(oldItem: ProductData, newItem: ProductData): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ProductData, newItem: ProductData): Boolean {
            return oldItem == newItem
        }
    }
}
