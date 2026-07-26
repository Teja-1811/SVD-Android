package com.svd.svdagencies.ui.user.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.user.UserBill

class UserBillsAdapter(
    private var bills: List<UserBill>,
    private val onAction: (UserBill, String) -> Unit
) : RecyclerView.Adapter<UserBillsAdapter.BillViewHolder>() {

    fun updateData(newBills: List<UserBill>) {
        this.bills = newBills
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_bill_card, parent, false)
        return BillViewHolder(view)
    }

    override fun onBindViewHolder(holder: BillViewHolder, position: Int) {
        val bill = bills[position]
        holder.bind(bill)
    }

    override fun getItemCount(): Int = bills.size

    inner class BillViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvInvoiceNum: TextView = itemView.findViewById(R.id.tvInvoiceNumber)
        private val tvDate: TextView = itemView.findViewById(R.id.tvInvoiceDate)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)
        private val tvOpDue: TextView = itemView.findViewById(R.id.tvOpDueValue)
        private val btnDownload: ImageButton = itemView.findViewById(R.id.btnDownloadBill)
        private val btnView: MaterialButton = itemView.findViewById(R.id.btnViewBill)
        private val btnQR: ImageButton = itemView.findViewById(R.id.btnQR)

        fun bind(bill: UserBill) {
            tvInvoiceNum.text = "#${bill.invoiceNumber}"
            tvDate.text = bill.invoiceDate
            tvAmount.text = "₹%.2f".format(bill.totalAmount)
            tvOpDue.text = "₹%.2f".format(bill.openingDue)

            itemView.setOnClickListener { onAction(bill, "view") }
            btnView.setOnClickListener { onAction(bill, "view") }
            btnDownload.setOnClickListener { onAction(bill, "download") }
            btnQR.setOnClickListener { onAction(bill, "qr") }
        }
    }
}
