package com.svd.svdagencies.ui.delivery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.delivery.DeliveryBillItem

class DeliveryBillConfirmationAdapter(
    private val items: List<Pair<DeliveryBillItem, Int>>
) : RecyclerView.Adapter<DeliveryBillConfirmationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvItemName: TextView = view.findViewById(R.id.tvProductName)
        val tvItemTotal: TextView = view.findViewById(R.id.tvProductTotal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.customer_order_summary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (item, qty) = items[position]
        holder.tvItemName.text = "${item.name} x $qty"
        val total = item.price * qty
        holder.tvItemTotal.text = "₹ %.2f".format(total)
    }

    override fun getItemCount() = items.size
}
