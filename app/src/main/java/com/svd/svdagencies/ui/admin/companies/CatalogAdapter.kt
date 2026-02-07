package com.svd.svdagencies.ui.admin.companies

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.data.model.admin.CatalogItem
import com.svd.svdagencies.databinding.AdminCatalogItemCardBinding
import java.text.NumberFormat
import java.util.Locale

class CatalogAdapter : ListAdapter<CatalogItem, CatalogAdapter.CatalogViewHolder>(CatalogDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatalogViewHolder {
        val binding = AdminCatalogItemCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CatalogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CatalogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CatalogViewHolder(private val binding: AdminCatalogItemCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CatalogItem) {
            binding.tvItemName.text = item.name
            
            val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            binding.tvMrp.text = formatter.format(item.mrp)
            binding.tvSellingPrice.text = formatter.format(item.sellingPrice)

            val margin = item.mrp - item.sellingPrice
            binding.tvMargin.text = formatter.format(margin)
        }
    }

    class CatalogDiffCallback : DiffUtil.ItemCallback<CatalogItem>() {
        override fun areItemsTheSame(oldItem: CatalogItem, newItem: CatalogItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CatalogItem, newItem: CatalogItem): Boolean {
            return oldItem == newItem
        }
    }
}
