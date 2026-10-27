package com.svd.svdagencies.ui.customer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.Company
import com.svd.svdagencies.databinding.CustomerCompanyCardBinding

class CustomerCompaniesAdapter(
    private val onViewCatalogClick: (Company) -> Unit
) : ListAdapter<Company, CustomerCompaniesAdapter.CompanyViewHolder>(CompanyDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompanyViewHolder {
        val binding = CustomerCompanyCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CompanyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CompanyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CompanyViewHolder(private val binding: CustomerCompanyCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(company: Company) {
            binding.tvCompanyName.text = company.name
            binding.tvWebsite.text = "Browse products"
            binding.tvItemCount.text = company.totalItems.toString()

            if (!company.logo.isNullOrEmpty()) {
                val imageUrl = ApiClient.getLogoUrl(company.logo)
                Glide.with(binding.ivCompanyLogo.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_milk_placeholder)
                    .into(binding.ivCompanyLogo)
            } else {
                binding.ivCompanyLogo.setImageResource(R.drawable.ic_milk_placeholder)
            }

            binding.tvWebsite.setOnClickListener { onViewCatalogClick(company) }

            binding.btnItems.setOnClickListener { onViewCatalogClick(company) }
        }
    }

    class CompanyDiffCallback : DiffUtil.ItemCallback<Company>() {
        override fun areItemsTheSame(oldItem: Company, newItem: Company): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Company, newItem: Company): Boolean {
            return oldItem == newItem
        }
    }
}