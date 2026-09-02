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
    private val onDeleteClick: (AdminBill) -> Unit,
    private val onQRClick: (AdminBill) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var isLoadingVisible = false

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_LOADING = 1
    }

    fun updateList(newBills: List<AdminBill>) {
        bills = newBills
        isLoadingVisible = false
        notifyDataSetChanged()
    }

    fun addItems(newBills: List<AdminBill>) {
        val startPosition = bills.size
        bills = bills + newBills
        notifyItemRangeInserted(startPosition, newBills.size)
    }

    fun setLoading(loading: Boolean) {
        if (isLoadingVisible == loading) return
        isLoadingVisible = loading
        if (loading) {
            notifyItemInserted(bills.size)
        } else {
            notifyItemRemoved(bills.size)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == bills.size) VIEW_TYPE_LOADING else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_LOADING) {
            val view = inflater.inflate(R.layout.item_loading_footer, parent, false)
            LoadingViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.admin_bill_card, parent, false)
            BillViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is BillViewHolder) {
            holder.bind(bills[position])
        }
    }

    override fun getItemCount(): Int = bills.size + if (isLoadingVisible) 1 else 0

    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class BillViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBillNumber: TextView = itemView.findViewById(R.id.tvBillNumber)
        private val tvBillDate: TextView = itemView.findViewById(R.id.tvBillDate)
        private val tvCustomerName: TextView = itemView.findViewById(R.id.tvCustomerName)
        private val tvGeneratedBy: TextView = itemView.findViewById(R.id.tvGeneratedBy)
        private val tvTotalAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)
        private val tvProfitAmount: TextView = itemView.findViewById(R.id.tvProfitAmount)
        private val btnView: ImageButton = itemView.findViewById(R.id.btnView)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val btnWhatsapp: ImageButton = itemView.findViewById(R.id.btnWhatsapp)
        private val btnDownload: ImageButton = itemView.findViewById(R.id.btnDownload)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        private val btnQR: ImageButton = itemView.findViewById(R.id.btnQR)

        fun bind(bill: AdminBill) {
            tvBillNumber.text = "Bill #" + (bill.bill_number ?: "")
            tvBillDate.text = formatDate(bill.date)
            tvCustomerName.text = bill.customer_name ?: ""
            tvGeneratedBy.text = bill.generatedBy?.name?.takeIf { it.isNotBlank() } ?: "Not recorded"
            tvTotalAmount.text = "₹" + bill.total_amount
            tvProfitAmount.text = "₹" + bill.profit

            btnView.setOnClickListener { onViewClick(bill) }
            btnEdit.setOnClickListener { onEditClick(bill) }
            btnWhatsapp.setOnClickListener { onWhatsappClick(bill) }
            btnDownload.setOnClickListener { onDownloadClick(bill) }
            btnDelete.setOnClickListener { onDeleteClick(bill) }
            btnQR.setOnClickListener { onQRClick(bill) }
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
