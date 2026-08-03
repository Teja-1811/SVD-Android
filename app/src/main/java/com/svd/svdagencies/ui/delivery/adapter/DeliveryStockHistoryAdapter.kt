package com.svd.svdagencies.ui.delivery.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.data.model.delivery.DeliveryStockHistoryItem
import com.svd.svdagencies.databinding.DeliveryStockHistoryBinding
import java.text.SimpleDateFormat
import java.util.Locale

class DeliveryStockHistoryAdapter(
    private val onItemClick: (DeliveryStockHistoryItem) -> Unit
) : RecyclerView.Adapter<DeliveryStockHistoryAdapter.ViewHolder>() {

    private var items: List<DeliveryStockHistoryItem> = emptyList()
    private val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

    fun submitList(newList: List<DeliveryStockHistoryItem>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DeliveryStockHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: DeliveryStockHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DeliveryStockHistoryItem) {
            val date = try {
                val d = apiFormat.parse(item.date)
                displayFormat.format(d!!)
            } catch (e: Exception) {
                item.date
            }

            binding.tvDate.text = date
            
            val net = (item.morningStock + item.eveningStock) - (item.morningReturn + item.eveningReturn)
            binding.tvTotalNet.text = "Net: ${formatQty(net)}"
            
            binding.tvMorning.text = "Mng: ${formatQty(item.morningStock)} / ${formatQty(item.morningReturn)}"
            binding.tvEvening.text = "Eve: ${formatQty(item.eveningStock)} / ${formatQty(item.eveningReturn)}"

            binding.root.setOnClickListener { onItemClick(item) }
        }

        private fun formatQty(value: Double): String {
            return if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)
        }
    }
}
