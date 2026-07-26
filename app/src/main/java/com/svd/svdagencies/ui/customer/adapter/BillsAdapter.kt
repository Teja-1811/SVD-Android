package com.svd.svdagencies.ui.customer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.customer.InvoiceItem

class BillsAdapter(
    private val bills: MutableList<InvoiceItem>,
    private val onAction: (InvoiceItem, String) -> Unit
) : RecyclerView.Adapter<BillsAdapter.BillViewHolder>() {

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

        holder.tvInvoiceNumber.text = bill.number
        holder.tvBillDate.text = bill.date
        holder.tvTotalAmount.text = "₹%.2f".format(bill.amount)

        holder.btnDownload.setOnClickListener { onAction(bill, "download") }
        holder.btnQR.setOnClickListener { onAction(bill, "qr") }
    }

    override fun getItemCount(): Int = bills.size
}
