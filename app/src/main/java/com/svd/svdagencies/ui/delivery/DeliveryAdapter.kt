package com.svd.svdagencies.ui.delivery

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.delivery.DeliveryItem
import com.svd.svdagencies.databinding.ItemDeliveryBinding
import java.util.Locale

class DeliveryAdapter(
    private val onUpdateStatus: (DeliveryItem, String) -> Unit,
    private val onQuickBill: (DeliveryItem) -> Unit
) : RecyclerView.Adapter<DeliveryAdapter.ViewHolder>() {

    private var items = listOf<DeliveryItem>()

    fun submitList(newList: List<DeliveryItem>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeliveryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemDeliveryBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DeliveryItem) {
            binding.tvCustomerName.text = item.customerName
            binding.tvType.text = item.type.asTypeLabel()
            binding.tvStatus.text = item.status.asStatusLabel()
            binding.tvPrimaryMeta.text = item.primaryMeta()
            binding.tvSecondaryMeta.text = item.secondaryMeta()
            binding.tvSchedule.text = item.deliveryDate ?: item.date ?: "Today"
            binding.tvStopNote.text = item.stopNote()

            val address = item.address.orEmpty().trim()
            binding.layoutAddress.visibility = if (address.isBlank()) View.GONE else View.VISIBLE
            binding.tvAddress.text = address

            applyTypeStyle(item)
            applyStatusStyle(item)
            bindActions(item)
        }

        private fun applyTypeStyle(item: DeliveryItem) {
            val color = if (item.type.equals("order", ignoreCase = true)) {
                R.color.brand_red
            } else {
                R.color.brand_blue
            }
            binding.tvType.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(binding.root.context, color)
            )
        }

        private fun applyStatusStyle(item: DeliveryItem) {
            val color = when (item.status.lowercase(Locale.getDefault())) {
                "pending" -> R.color.icon_orange
                "out_for_delivery" -> R.color.brand_blue
                "delivered" -> R.color.icon_green
                else -> R.color.gray500
            }
            binding.tvStatus.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(binding.root.context, color)
            )
        }

        private fun bindActions(item: DeliveryItem) {
            if (item.status.equals("delivered", ignoreCase = true)) {
                binding.layoutActions.visibility = View.GONE
                return
            }

            binding.layoutActions.visibility = View.VISIBLE
            binding.btnOutForDelivery.visibility =
                if (item.status.equals("pending", ignoreCase = true)) View.VISIBLE else View.GONE

            binding.btnOutForDelivery.setOnClickListener {
                onUpdateStatus(item, "out_for_delivery")
            }
            binding.btnDelivered.setOnClickListener {
                onUpdateStatus(item, "delivered")
            }
            binding.btnQuickBill.setOnClickListener {
                onQuickBill(item)
            }
        }
    }
}

private fun DeliveryItem.primaryMeta(): String {
    return if (type.equals("order", ignoreCase = true)) {
        "Order #${orderNumber ?: id}"
    } else {
        planItem ?: "Subscription delivery"
    }
}

private fun DeliveryItem.secondaryMeta(): String {
    return if (type.equals("order", ignoreCase = true)) {
        "Collect ${formatCurrency(totalAmount)}"
    } else {
        "Quantity ${quantity ?: 0}"
    }
}

private fun DeliveryItem.stopNote(): String {
    return when {
        type.equals("order", ignoreCase = true) && !address.isNullOrBlank() -> "Address confirmed"
        type.equals("order", ignoreCase = true) -> "Order delivery"
        status.equals("out_for_delivery", ignoreCase = true) -> "On the way"
        else -> "Subscription drop"
    }
}

private fun String.asTypeLabel(): String = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
}

private fun String.asStatusLabel(): String = replace("_", " ").uppercase(Locale.getDefault())

private fun formatCurrency(amount: Double?): String {
    return if (amount == null) {
        "Rs. 0.00"
    } else {
        String.format(Locale.getDefault(), "Rs. %.2f", amount)
    }
}
