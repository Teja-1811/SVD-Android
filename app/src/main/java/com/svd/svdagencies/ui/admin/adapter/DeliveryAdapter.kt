package com.svd.svdagencies.ui.admin.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.admin.Delivery
import com.svd.svdagencies.databinding.AdminDeliveryRowBinding

class DeliveryAdapter(private var deliveries: List<Delivery>) : RecyclerView.Adapter<DeliveryAdapter.ViewHolder>() {

    private val deliveredIds = mutableSetOf<Int>()

    class ViewHolder(val binding: AdminDeliveryRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AdminDeliveryRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val delivery = deliveries[position]
        holder.binding.tvCustomerName.text = delivery.customer
        holder.binding.tvPlanName.text = delivery.plan
        holder.binding.tvItems.text = "Start: ${delivery.startDate}\nEnd: ${delivery.endDate}"
        holder.binding.tvPhone.text = delivery.phone
        holder.binding.layoutHeader.visibility = if (position == 0) View.VISIBLE else View.GONE

        val isDelivered = deliveredIds.contains(delivery.subscriptionId)
        bindStatus(holder, isDelivered)

        holder.binding.btnStatusUpdate.setOnClickListener {
            if (isDelivered) {
                deliveredIds.remove(delivery.subscriptionId)
            } else {
                deliveredIds.add(delivery.subscriptionId)
            }
            notifyItemChanged(position)
        }
    }

    override fun getItemCount() = deliveries.size

    fun updateData(newDeliveries: List<Delivery>) {
        deliveries = newDeliveries
        notifyDataSetChanged()
    }

    private fun bindStatus(holder: ViewHolder, isDelivered: Boolean) {
        val context = holder.binding.root.context
        val green = ContextCompat.getColor(context, R.color.icon_green)
        val greenBg = ContextCompat.getColor(context, R.color.bg_light_green)
        val blue = ContextCompat.getColor(context, R.color.icon_blue)
        val blueBg = ContextCompat.getColor(context, R.color.bg_light_gray)

        if (isDelivered) {
            holder.binding.tvStatus.text = "Delivered"
            holder.binding.tvStatus.setTextColor(green)
            holder.binding.tvStatus.backgroundTintList = ColorStateList.valueOf(greenBg)
            holder.binding.btnStatusUpdate.text = "Delivered"
            holder.binding.btnStatusUpdate.iconTint = ColorStateList.valueOf(green)
            holder.binding.btnStatusUpdate.strokeColor = ColorStateList.valueOf(green)
            holder.binding.btnStatusUpdate.setTextColor(green)
            holder.binding.btnStatusUpdate.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.transparent))
        } else {
            holder.binding.tvStatus.text = "Pending"
            holder.binding.tvStatus.setTextColor(blue)
            holder.binding.tvStatus.backgroundTintList = ColorStateList.valueOf(blueBg)
            holder.binding.btnStatusUpdate.text = "Mark Delivered"
            holder.binding.btnStatusUpdate.iconTint = ColorStateList.valueOf(green)
            holder.binding.btnStatusUpdate.strokeColor = ColorStateList.valueOf(green)
            holder.binding.btnStatusUpdate.setTextColor(green)
            holder.binding.btnStatusUpdate.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.transparent))
        }
    }
}
