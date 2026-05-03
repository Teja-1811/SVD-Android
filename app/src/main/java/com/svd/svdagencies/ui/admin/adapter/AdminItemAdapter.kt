package com.svd.svdagencies.ui.admin.adapter

import android.content.Intent
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.Items.AdminItem
import com.svd.svdagencies.data.model.admin.Bills.BillItem
import com.svd.svdagencies.ui.admin.bills.AdminBillDetailActivity

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
            .inflate(R.layout.admin_item_card, parent, false)
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
        private val btnUpdate: MaterialButton = itemView.findViewById(R.id.btnUpdate)
        private val btnStatus: MaterialButton = itemView.findViewById(R.id.btnStatus)
        private val imgCompanyLogo: ImageView = itemView.findViewById(R.id.imgCompanyLogo)
        private val imgProduct: ImageView = itemView.findViewById(R.id.imgProduct)

        fun bind(item: AdminItem) {
            tvProductName.text = item.name
            tvCode.text = item.code
            tvCompany.text = item.company
            tvBuyingPrice.text = "₹%.2f".format(item.buyingPriceValue)
            tvSellingPrice.text = "₹%.2f".format(item.sellingPriceValue)
            tvMrp.text = "₹%.2f".format(item.mrpValue)
            tvMargin.text = "₹%.2f".format(item.margin)
            tvStock.text = "${item.stock_quantity ?: 0} in stock"

            // Update status button based on frozen state
            if (item.frozen) {
                btnStatus.text = "Activate"
                btnStatus.setIconResource(R.drawable.ic_unlock)
                val greenColor = ContextCompat.getColor(itemView.context, R.color.icon_green)
                btnStatus.setTextColor(greenColor)
                btnStatus.strokeColor = ColorStateList.valueOf(greenColor)
                btnStatus.iconTint = ColorStateList.valueOf(greenColor)
            } else {
                btnStatus.text = "Deactivate"
                btnStatus.setIconResource(R.drawable.ic_lock)
                val redColor = ContextCompat.getColor(itemView.context, R.color.brand_red)
                btnStatus.setTextColor(redColor)
                btnStatus.strokeColor = ColorStateList.valueOf(redColor)
                btnStatus.iconTint = ColorStateList.valueOf(redColor)
            }

            // Loading company logo using Glide
            val companyLogoUrl = ApiClient.getLogoUrl(item.logo)
            if (companyLogoUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(companyLogoUrl)
                    .placeholder(R.drawable.ic_milk_placeholder)
                    .error(R.drawable.ic_milk_placeholder)
                    .into(imgCompanyLogo)
            } else {
                imgCompanyLogo.setImageResource(R.drawable.ic_milk_placeholder)
            }

            // Loading product image using Glide
            val imageUrl = ApiClient.getImageUrl(item.image)
            
            if (imageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_milk_placeholder)
                    .error(R.drawable.ic_milk_placeholder)
                    .into(imgProduct)
            } else {
                imgProduct.setImageResource(R.drawable.ic_milk_placeholder)
            }

            btnUpdate.setOnClickListener { onEditClick(item) }
            btnStatus.setOnClickListener { onFreezeClick(item) }
        }
    }
}

class BillAdapter(private var bills: List<BillItem>) : RecyclerView.Adapter<BillAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.admin_bill_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bill = bills[position]
        holder.bind(bill)
    }

    override fun getItemCount(): Int = bills.size

    fun updateData(newBills: List<BillItem>) {
        bills = newBills
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCustomerName: TextView = itemView.findViewById(R.id.tvCustomerName)
        private val tvBillDate: TextView = itemView.findViewById(R.id.tvBillDate)
        private val tvBillNumber: TextView = itemView.findViewById(R.id.tvBillNumber)
        private val tvTotalAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)
        private val btnView: ImageButton = itemView.findViewById(R.id.btnView)

        fun bind(bill: BillItem) {
            tvCustomerName.text = bill.customer
            tvBillDate.text = bill.invoice_date
            tvBillNumber.text = bill.invoice_number
            tvTotalAmount.text = "₹${bill.total_amount}"

            val navigateToDetail = {
                val context = itemView.context
                val intent = Intent(context, AdminBillDetailActivity::class.java).apply {
                    putExtra("bill_id", bill.id)
                }
                context.startActivity(intent)
            }

            btnView.setOnClickListener { navigateToDetail() }
            itemView.setOnClickListener { navigateToDetail() }
        }
    }
}
