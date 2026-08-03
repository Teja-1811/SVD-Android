package com.svd.svdagencies.ui.delivery.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.delivery.DeliveryStockDashboardItem

class DeliveryStockReportAdapter : RecyclerView.Adapter<DeliveryStockReportAdapter.ViewHolder>() {

    private var items: List<DeliveryStockDashboardItem> = emptyList()

    fun getItems(): List<DeliveryStockDashboardItem> = items

    fun submitList(newItems: List<DeliveryStockDashboardItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.delivery_stock_report, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.tvItemName)
        private val ivItem: ImageView = view.findViewById(R.id.ivItem)
        private val tvMngEntry: TextView = view.findViewById(R.id.tvMngEntry)
        private val tvMngReturn: TextView = view.findViewById(R.id.tvMngReturn)
        private val tvEveEntry: TextView = view.findViewById(R.id.tvEveEntry)
        private val tvEveReturn: TextView = view.findViewById(R.id.tvEveReturn)
        private val tvTotalTaken: TextView = view.findViewById(R.id.tvTotalTaken)
        private val tvTotalBilled: TextView = view.findViewById(R.id.tvTotalBilled)
        private val tvRemaining: TextView = view.findViewById(R.id.tvRemainingQty)

        fun bind(item: DeliveryStockDashboardItem) {
            tvName.text = item.itemName

            val mainUrl: String?
            val fallbackUrl: String?

            if (!item.image.isNullOrEmpty()) {
                mainUrl = ApiClient.getImageUrl(item.image)
                fallbackUrl = null
            } else if (!item.itemCode.isNullOrEmpty()) {
                mainUrl = ApiClient.getImageUrl("${item.itemCode}.png")
                fallbackUrl = ApiClient.getImageUrl("${item.itemCode}.jpg")
            } else {
                mainUrl = null
                fallbackUrl = null
            }

            Glide.with(itemView.context)
                .load(mainUrl)
                .placeholder(R.drawable.ic_milk_placeholder)
                .error(
                    Glide.with(itemView.context)
                        .load(fallbackUrl)
                        .error(R.drawable.ic_milk_placeholder)
                )
                .into(ivItem)

            tvMngEntry.text = formatQty(item.morningStock)
            tvMngReturn.text = formatQty(item.morningReturn)
            tvEveEntry.text = formatQty(item.eveningStock)
            tvEveReturn.text = formatQty(item.eveningReturn)
            
            tvTotalTaken.text = formatQty(item.totalStock)
            tvTotalBilled.text = formatQty(item.billedQty)
            tvRemaining.text = formatQty(item.difference)
        }

        private fun formatQty(value: Double): String {
            return if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)
        }
    }
}
