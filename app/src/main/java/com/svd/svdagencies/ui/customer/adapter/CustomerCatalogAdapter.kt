package com.svd.svdagencies.ui.customer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.data.model.admin.CatalogItem
import com.svd.svdagencies.databinding.CustomerCatalogItemCardBinding
import java.text.NumberFormat
import java.util.Locale

class CustomerCatalogAdapter : ListAdapter<CatalogItem, CustomerCatalogAdapter.CatalogViewHolder>(CatalogDiffCallback()) {

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

        fun bind(item: CatalogItem) {
            binding.tvProductName.text = item.name
            
            val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            binding.tvPrice.text = formatter.format(item.sellingPrice)
            
            // Optional: Handle other fields if they exist in the model
            // binding.tvCompany.text = ...
            // binding.tvUnit.text = ...
            
            // Setup quantity controls if needed
            binding.tvQty.text = "0"
            binding.btnPlus.setOnClickListener {
                val currentQty = binding.tvQty.text.toString().toInt()
                binding.tvQty.text = (currentQty + 1).toString()
            }
            binding.btnMinus.setOnClickListener {
                val currentQty = binding.tvQty.text.toString().toInt()
                if (currentQty > 0) {
                    binding.tvQty.text = (currentQty - 1).toString()
                }
            }
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
