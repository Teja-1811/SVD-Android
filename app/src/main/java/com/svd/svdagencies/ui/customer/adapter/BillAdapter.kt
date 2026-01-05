package com.svd.svdagencies.ui.customer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.customer.Bill

class BillAdapter(
    private val bills: List<Bill>,
    private val onAction: (Bill, String) -> Unit
) : RecyclerView.Adapter<BillAdapter.BillViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            BillViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bill, parent, false)
        return BillViewHolder(view)
    }

    override fun onBindViewHolder(holder: BillViewHolder, position: Int) {
        val bill = bills[position]
        holder.bind(bill)
    }

    override fun getItemCount(): Int = bills.size

    inner class BillViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBillNo: TextView = itemView.findViewById(R.id.tvBillNo)
        private val tvBillDate: TextView = itemView.findViewById(R.id.tvBillDate)
        private val tvBillAmount: TextView = itemView.findViewById(R.id.tvBillAmount)
        private val btnDownloadBill: Button = itemView.findViewById(R.id.btnDownloadBill)

        fun bind(bill: Bill) {
            tvBillNo.text = "Bill ID: #${bill.id}"
            tvBillDate.text = bill.date
            tvBillAmount.text = "Amount: ₹${bill.amount}"

            btnDownloadBill.setOnClickListener { onAction(bill, "download") }
        }
    }
}
