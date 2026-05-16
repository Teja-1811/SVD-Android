package com.svd.svdagencies.ui.delivery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.delivery.DeliveryTodayBill
import java.util.Locale

class DeliveryTodayBillAdapter(
    private val onViewBill: (DeliveryTodayBill) -> Unit,
    private val onDeleteBill: (DeliveryTodayBill) -> Unit
) : RecyclerView.Adapter<DeliveryTodayBillAdapter.ViewHolder>() {

    private var items = listOf<DeliveryTodayBill>()

    fun submitList(newList: List<DeliveryTodayBill>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_delivery_today_bill, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvBillNumber: TextView = view.findViewById(R.id.tvBillNumber)
        private val tvBillDate: TextView = view.findViewById(R.id.tvBillDate)
        private val tvBillAmount: TextView = view.findViewById(R.id.tvBillAmount)
        private val btnViewBill: MaterialButton = view.findViewById(R.id.btnViewBill)
        private val btnDeleteBill: MaterialButton = view.findViewById(R.id.btnDeleteBill)

        fun bind(item: DeliveryTodayBill) {
            tvBillNumber.text = item.billNumber ?: "#${item.realId}"
            tvBillDate.text = item.date
            tvBillAmount.text = String.format(Locale.getDefault(), "Rs. %.2f", item.totalAmount)
            btnViewBill.setOnClickListener { onViewBill(item) }
            btnDeleteBill.setOnClickListener { onDeleteBill(item) }
        }
    }
}
