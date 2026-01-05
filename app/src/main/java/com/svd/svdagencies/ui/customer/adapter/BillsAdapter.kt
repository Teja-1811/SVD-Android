package com.svd.svdagencies.ui.customer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
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
        val tvBillNo: TextView = itemView.findViewById(R.id.tvBillNo)
        val tvBillDate: TextView = itemView.findViewById(R.id.tvBillDate)
        val tvBillAmount: TextView = itemView.findViewById(R.id.tvBillAmount)
        val btnDownload: MaterialButton = itemView.findViewById(R.id.btnDownloadBill)
    }

    // ================= REQUIRED METHODS =================
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bill, parent, false)
        return BillViewHolder(view)
    }

    override fun onBindViewHolder(holder: BillViewHolder, position: Int) {
        val bill = bills[position]

        holder.tvBillNo.text = bill.number
        holder.tvBillDate.text = bill.date
        holder.tvBillAmount.text = "₹%.2f".format(bill.amount)

        holder.btnDownload.setOnClickListener { onAction(bill, "download") }
    }

    override fun getItemCount(): Int = bills.size
}
