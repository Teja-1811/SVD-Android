package com.svd.svdagencies.ui.admin.companies

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.Company
import com.svd.svdagencies.databinding.AdminCompanyCardBinding
import java.text.NumberFormat
import java.util.Locale

class CompaniesAdapter(
    private val onEditClick: (Company) -> Unit,
    private val onViewCatalogClick: (Company) -> Unit
) : ListAdapter<Company, CompaniesAdapter.CompanyViewHolder>(CompanyDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompanyViewHolder {
        val binding = AdminCompanyCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CompanyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CompanyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CompanyViewHolder(private val binding: AdminCompanyCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(company: Company) {
            binding.tvCompanyName.text = company.name
            binding.tvWebsite.text = company.websiteLink ?: "No website"
            binding.tvItemCount.text = company.totalItems.toString()
            binding.tvTotalStock.text = company.totalQty.toString()
            
            val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            binding.tvStockValue.text = formatter.format(company.totalValue)

            if (!company.logo.isNullOrEmpty()) {
                val imageUrl = if (company.logo.startsWith("http")) {
                    company.logo
                } else {
                    ApiClient.BASE_URL + company.logo.removePrefix("/")
                }
                Glide.with(binding.ivCompanyLogo.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_milk_placeholder)
                    .into(binding.ivCompanyLogo)
            } else {
                binding.ivCompanyLogo.setImageResource(R.drawable.ic_milk_placeholder)
            }

            binding.btnEdit.setOnClickListener { onEditClick(company) }
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
