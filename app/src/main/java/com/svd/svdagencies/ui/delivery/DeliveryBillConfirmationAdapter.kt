package com.svd.svdagencies.ui.delivery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.delivery.DeliveryBillItem

class DeliveryBillConfirmationAdapter(
    private val items: List<Pair<DeliveryBillItem, Int>>,
    private val userType: String = "user",
    private val discountForItem: (DeliveryBillItem, Int) -> Double
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
        val discount = discountForItem(item, qty)
        val basePrice = if (userType == "user") item.mrp else item.sellingPrice
        val finalPrice = (basePrice - discount).coerceAtLeast(0.0)
        val total = finalPrice * qty

        holder.tvItemName.text = if (discount > 0.0) {
            "%s x %d\n\u20B9 %.2f/qty - \u20B9 %.2f discount = \u20B9 %.2f/qty"
                .format(item.name, qty, basePrice, discount, finalPrice)
        } else {
            "%s x %d\n\u20B9 %.2f/qty".format(item.name, qty, basePrice)
        }
        holder.tvItemTotal.text = "\u20B9 %.2f".format(total)
    }

    override fun getItemCount() = items.size
}
