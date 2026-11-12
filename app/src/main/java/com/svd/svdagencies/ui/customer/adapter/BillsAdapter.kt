package com.svd.svdagencies.ui.customer.adapter

import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.customer.InvoiceItem

class BillsAdapter(
    private val bills: MutableList<InvoiceItem>,
    private val onAction: (InvoiceItem, String) -> Unit
) : RecyclerView.Adapter<BillsAdapter.BillViewHolder>() {

    private var paymentLocked = true
    fun setPaymentLocked(locked: Boolean) {
        paymentLocked = locked
        notifyDataSetChanged()
    }

    // ================= UPDATE DATA =================
    fun updateData(newBills: List<InvoiceItem>) {
        bills.clear()
        bills.addAll(newBills)
        notifyDataSetChanged()
    }

    // ================= VIEW HOLDER =================
    inner class BillViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvInvoiceNumber: TextView = itemView.findViewById(R.id.tvInvoiceNumber)
        val tvBillDate: TextView = itemView.findViewById(R.id.tvInvoiceDate)
        val tvTotalAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvOpeningDue: TextView = itemView.findViewById(R.id.tvOpeningDue)
        val tvPaidAmount: TextView = itemView.findViewById(R.id.tvPaidAmount)
        val tvRemainingDue: TextView = itemView.findViewById(R.id.tvRemainingDue)
        val itemTable: LinearLayout = itemView.findViewById(R.id.layoutInvoiceItems)
        val btnDownload: View = itemView.findViewById(R.id.btnDownloadBill)
        val btnQR: View = itemView.findViewById(R.id.btnQR)
    }

    // ================= REQUIRED METHODS =================
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.customer_invoice_card, parent, false)
        return BillViewHolder(view)
    }

    override fun onBindViewHolder(holder: BillViewHolder, position: Int) {
        val bill = bills[position]

        holder.tvInvoiceNumber.text = bill.number.orEmpty()
        holder.tvBillDate.text = bill.date.orEmpty()
        holder.tvTotalAmount.text = "Billed ${money(bill.amount)}"
        holder.tvOpeningDue.text = "Opening ${money(bill.openingDue)}"
        holder.tvPaidAmount.text = "Paid ${money(bill.paid)}"
        holder.tvRemainingDue.text = "Balance due ${money(bill.remainingDue)}"
        bindStatus(holder.tvStatus, bill)
        bindItems(holder.itemTable, bill)

        holder.btnDownload.setOnClickListener { onAction(bill, "download") }
        holder.btnQR.isEnabled = !paymentLocked && bill.remainingDue > 0
        holder.btnQR.alpha = if (holder.btnQR.isEnabled) 1f else 0.5f
        holder.btnQR.setOnClickListener { onAction(bill, "qr") }
    }

    override fun getItemCount(): Int = bills.size

    private fun bindStatus(view: TextView, bill: InvoiceItem) {
        val (label, color, background) = when {
            bill.remainingDue <= 0.0 -> Triple("PAID", R.color.icon_green, android.graphics.Color.parseColor("#EAF6F0"))
            bill.paid > 0.0 -> Triple("PART PAID", R.color.icon_orange, android.graphics.Color.parseColor("#FFF5D9"))
            else -> Triple("UNPAID", R.color.brand_red, android.graphics.Color.parseColor("#FFEDEF"))
        }
        view.text = label
        view.setTextColor(ContextCompat.getColor(view.context, color))
        view.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(background)
            cornerRadius = dp(view, 8).toFloat()
        }
    }

    private fun bindItems(container: LinearLayout, bill: InvoiceItem) {
        container.removeAllViews()
        container.addView(tableRow(container, listOf("Item", "Qty", "Rate", "Disc", "Total"), true))
        val items = bill.items.orEmpty()
        if (items.isEmpty()) {
            container.addView(tableText(container, "Item details are available in the downloaded invoice.", 12f, false).apply {
                setPadding(0, dp(container, 8), 0, 0)
            })
            return
        }
        items.forEach { line ->
            container.addView(tableRow(container, listOf(
                line.code.orEmpty().ifBlank { "—" }, clean(line.quantity), money(line.rate), money(line.discount), money(line.total)
            ), false))
        }
    }

    private fun tableRow(parent: LinearLayout, values: List<String>, heading: Boolean): LinearLayout = LinearLayout(parent.context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, if (heading) 0 else dp(parent, 7), 0, 0)
        val weights = floatArrayOf(1.35f, .48f, .95f, .9f, 1.12f)
        values.forEachIndexed { index, value ->
            addView(tableText(parent, value, if (heading) 10f else 11f, heading).apply {
                gravity = if (index == 0) Gravity.START else Gravity.END
                if (heading) setTextColor(ContextCompat.getColor(context, R.color.textColorSecondary))
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weights[index]))
        }
    }

    private fun tableText(parent: LinearLayout, value: String, size: Float, bold: Boolean): TextView = TextView(parent.context).apply {
        text = value
        textSize = size
        setTextColor(ContextCompat.getColor(context, if (bold) R.color.textColorPrimary else R.color.textColorSecondary))
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun money(value: Double): String = "₹%.2f".format(value)
    private fun clean(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
    private fun dp(view: View, value: Int): Int = (value * view.resources.displayMetrics.density).toInt()
}
