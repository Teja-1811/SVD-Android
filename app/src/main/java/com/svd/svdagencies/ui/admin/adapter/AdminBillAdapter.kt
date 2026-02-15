package com.svd.svdagencies.ui.admin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.admin.Bills.AdminBill
import java.text.SimpleDateFormat
import java.util.Locale

class AdminBillAdapter(
    private var bills: List<AdminBill>,
    private val onViewClick: (AdminBill) -> Unit,
    private val onEditClick: (AdminBill) -> Unit,
    private val onWhatsappClick: (AdminBill) -> Unit,
    private val onDownloadClick: (AdminBill) -> Unit,
    private val onDeleteClick: (AdminBill) -> Unit
) : RecyclerView.Adapter<AdminBillAdapter.BillViewHolder>() {

    fun updateList(newBills: List<AdminBill>) {
        bills = newBills
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.admin_bill_card, parent, false)
        return BillViewHolder(view)
    }

    override fun onBindViewHolder(holder: BillViewHolder, position: Int) {
        holder.bind(bills[position])
    }

    override fun getItemCount(): Int = bills.size

    inner class BillViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBillNumber: TextView = itemView.findViewById(R.id.tvBillNumber)
        private val tvBillDate: TextView = itemView.findViewById(R.id.tvBillDate)
        private val tvCustomerName: TextView = itemView.findViewById(R.id.tvCustomerName)
        private val tvTotalAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)
        private val tvProfitAmount: TextView = itemView.findViewById(R.id.tvProfitAmount)
        private val btnView: ImageButton = itemView.findViewById(R.id.btnView)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val btnWhatsapp: ImageButton = itemView.findViewById(R.id.btnWhatsapp)
        private val btnDownload: ImageButton = itemView.findViewById(R.id.btnDownload)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(bill: AdminBill) {
            tvBillNumber.text = "Bill #" + (bill.bill_number ?: "")
            tvBillDate.text = formatDate(bill.date)
            tvCustomerName.text = bill.customer_name ?: ""
            tvTotalAmount.text = "₹" + bill.total_amount
            tvProfitAmount.text = "₹" + bill.profit

            btnView.setOnClickListener { onViewClick(bill) }
            btnEdit.setOnClickListener { onEditClick(bill) }
            btnWhatsapp.setOnClickListener { onWhatsappClick(bill) }
            btnDownload.setOnClickListener { onDownloadClick(bill) }
            btnDelete.setOnClickListener { onDeleteClick(bill) }
        }

        private fun formatDate(dateString: String?): String {
            if (dateString == null) return ""
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                if (date != null) {
                    outputFormat.format(date)
                } else {
                    dateString
                }
            } catch (e: Exception) {
                dateString
            }
        }
    }
}
