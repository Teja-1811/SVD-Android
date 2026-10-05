package com.svd.svdagencies.ui.delivery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.TableLayout
import android.widget.TableRow
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.delivery.DeliveryTodayBill
import java.util.Locale

class DeliveryTodayBillAdapter(
    private val onViewBill: (DeliveryTodayBill) -> Unit,
    private val onDeleteBill: (DeliveryTodayBill) -> Unit,
    private val onShowQR: (DeliveryTodayBill) -> Unit,
    private val onShareWhatsapp: ((DeliveryTodayBill) -> Unit)? = null
) : RecyclerView.Adapter<DeliveryTodayBillAdapter.ViewHolder>() {

    private var items = listOf<DeliveryTodayBill>()

    fun submitList(newList: List<DeliveryTodayBill>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.delivery_today_bill, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvBillNumber: TextView = view.findViewById(R.id.tvBillNumber)
        private val tvBillDate: TextView = view.findViewById(R.id.tvBillDate)
        private val tvCustomerName: TextView = view.findViewById(R.id.tvCustomerName)
        private val tableBillItems: TableLayout = view.findViewById(R.id.tvBillItems)
        private val tvBillAmount: TextView = view.findViewById(R.id.tvBillAmount)
        private val tvOpeningDue: TextView = view.findViewById(R.id.tvOpeningDue)
        private val tvGrandTotal: TextView = view.findViewById(R.id.tvGrandTotal)
        private val tvPaidAmount: TextView = view.findViewById(R.id.tvPaidAmount)
        private val tvRemainingDue: TextView = view.findViewById(R.id.tvRemainingDue)
        private val btnQR: View = view.findViewById(R.id.btnQR)
        private val btnDeleteBill: View = view.findViewById(R.id.btnDeleteBill)
        private val btnWhatsapp: View? = view.findViewById(R.id.btnWhatsapp)

        fun bind(item: DeliveryTodayBill) {
            tvBillNumber.text = item.billNumber ?: "#${item.realId}"
            tvBillDate.text = item.date.orEmpty()
            tvCustomerName.text = item.customerName ?: item.customer ?: "Customer"
            bindItems(item)

            val grandTotal = item.invoiceAmount + item.openingDue
            tvBillAmount.text = money(item.invoiceAmount)
            tvOpeningDue.text = money(item.openingDue)
            tvGrandTotal.text = money(grandTotal)
            tvPaidAmount.text = money(item.paid)
            tvRemainingDue.text = money(item.remainingDue)
            btnQR.setOnClickListener { onShowQR(item) }
            btnDeleteBill.setOnClickListener { onDeleteBill(item) }
            btnWhatsapp?.setOnClickListener { onShareWhatsapp?.invoke(item) }
        }

        private fun money(amount: Double) = String.format(Locale.getDefault(), "₹%.2f", amount)

        private fun bindItems(item: DeliveryTodayBill) {
            while (tableBillItems.childCount > 1) tableBillItems.removeViewAt(1)
            item.items.forEach { line ->
                val row = TableRow(itemView.context).apply { setPadding(0, 5, 0, 5) }
                row.addView(cell(line.name ?: "Item", 0, false))
                row.addView(cell(String.format(Locale.getDefault(), "%.0f", line.quantity), 42, true))
                row.addView(cell(money(line.pricePerUnit), 62, true))
                row.addView(cell(if (line.discount > 0) money(line.discount) else "—", 58, true))
                row.addView(cell(money(line.totalAmount), 64, true))
                tableBillItems.addView(row)
            }
            if (item.items.isEmpty()) {
                tableBillItems.addView(TableRow(itemView.context).apply { addView(cell("No item details available", 0, false)) })
            }
        }

        private fun cell(text: String, widthDp: Int, end: Boolean): TextView = TextView(itemView.context).apply {
            this.text = text
            textSize = 12f
            setTextColor(itemView.context.getColor(R.color.textColorPrimary))
            gravity = if (end) android.view.Gravity.END else android.view.Gravity.START
            layoutParams = TableRow.LayoutParams(if (widthDp == 0) 0 else (widthDp * itemView.resources.displayMetrics.density).toInt(), TableRow.LayoutParams.WRAP_CONTENT, if (widthDp == 0) 1f else 0f)
        }
    }
}
