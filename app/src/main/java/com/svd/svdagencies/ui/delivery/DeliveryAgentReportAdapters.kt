package com.svd.svdagencies.ui.delivery

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.delivery.*
import java.util.Locale

class DeliveryAgentSummaryHeaderAdapter(
    private val onClear: () -> Unit,
    private val onApply: (View) -> Unit,
    private val onPickStartDate: () -> Unit,
    private val onPickEndDate: () -> Unit,
    private val onRecordSubmission: () -> Unit,
    private val onAgentSelected: () -> Unit
) : RecyclerView.Adapter<DeliveryAgentSummaryHeaderAdapter.ViewHolder>() {

    var summary: DeliveryAgentDuesSummary? = null
    var startDateStr: String = ""
    var endDateStr: String = ""
    var agentAdapter: ArrayAdapter<String>? = null
    var selectedAgentName: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.admin_delivery_agent_summary_header, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind()

    override fun getItemCount(): Int = 1

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val autoCompleteAgent: AutoCompleteTextView = itemView.findViewById(R.id.autoCompleteAgent)
        val tvStartDate: TextView = itemView.findViewById(R.id.tvStartDate)
        val tvEndDate: TextView = itemView.findViewById(R.id.tvEndDate)
        val tvBillCount: TextView = itemView.findViewById(R.id.tvBillCount)
        val tvTotalAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)
        val tvCounterDue: TextView = itemView.findViewById(R.id.tvCounterDue)
        val tvProfit: TextView = itemView.findViewById(R.id.tvProfit)
        val tvCustomerBillCount: TextView = itemView.findViewById(R.id.tvCustomerBillCount)
        val tvSelfBillCount: TextView = itemView.findViewById(R.id.tvSelfBillCount)
        val tvCustomerInvoiceAmount: TextView = itemView.findViewById(R.id.tvCustomerInvoiceAmount)
        val tvSelfInvoiceAmount: TextView = itemView.findViewById(R.id.tvSelfInvoiceAmount)
        val tvSubmittedAmount: TextView = itemView.findViewById(R.id.tvSubmittedAmount)
        val tvRemainingDue: TextView = itemView.findViewById(R.id.tvRemainingDue)
        val btnClear: View = itemView.findViewById(R.id.btnClear)
        val btnApply: View = itemView.findViewById(R.id.btnApply)
        val btnRecordSubmission: View = itemView.findViewById(R.id.btnRecordSubmission)

        init {
            btnClear.setOnClickListener { onClear() }
            btnApply.setOnClickListener { onApply(it) }
            tvStartDate.setOnClickListener { onPickStartDate() }
            tvEndDate.setOnClickListener { onPickEndDate() }
            btnRecordSubmission.setOnClickListener { onRecordSubmission() }
            autoCompleteAgent.setOnItemClickListener { _, _, _, _ ->
                selectedAgentName = autoCompleteAgent.text.toString()
                onAgentSelected()
            }
        }

        fun bind() {
            autoCompleteAgent.setAdapter(agentAdapter)
            if (autoCompleteAgent.text.toString() != selectedAgentName) {
                autoCompleteAgent.setText(selectedAgentName, false)
            }
            tvStartDate.text = startDateStr
            tvEndDate.text = endDateStr

            summary?.let { s ->
                tvBillCount.text = "Total Bills\n${s.generatedBillCount}"
                tvTotalAmount.text = "Total Invoice\n${money(s.generatedInvoiceAmount)}"
                tvCounterDue.text = "To Submit\n${money(s.amountToSubmit)}"
                tvProfit.text = "Company Profit\n${money(s.totalProfit)}"
                tvCustomerBillCount.text = "Customer Bills\n${s.customerBillCount}"
                tvSelfBillCount.text = "Self Bills\n${s.selfBillCount}"
                tvCustomerInvoiceAmount.text = "Customer Invoice\n${money(s.customerBillAmount)}"
                tvSelfInvoiceAmount.text = "Self Invoice\n${money(s.selfBillAmount)}"
                tvSubmittedAmount.text = "Submitted\n${money(s.submittedAmount)}"
                tvRemainingDue.text = "Remaining Due\n${money(s.remainingGeneratedAmount)}"
            }
        }
    }
}

class DeliveryAgentSectionHeaderAdapter(private val title: String) : RecyclerView.Adapter<DeliveryAgentSectionHeaderAdapter.ViewHolder>() {
    var isVisible: Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = TextView(parent.context).apply {
            val dp16 = (16 * parent.context.resources.displayMetrics.density).toInt()
            val dp8 = (8 * parent.context.resources.displayMetrics.density).toInt()
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(dp16, dp16, dp16, dp8)
            setTextColor(parent.context.getColor(R.color.textColorPrimary))
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
        }
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tv = holder.itemView as TextView
        tv.text = title
        tv.visibility = if (isVisible) View.VISIBLE else View.GONE
        tv.layoutParams.height = if (isVisible) ViewGroup.LayoutParams.WRAP_CONTENT else 0
    }
    override fun getItemCount(): Int = 1
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}

class DeliveryAgentDuesTableHeaderAdapter : RecyclerView.Adapter<DeliveryAgentDuesTableHeaderAdapter.ViewHolder>() {
    var isVisible: Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.admin_delivery_agent_due_row, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.date.text = "Date"
        holder.collected.text = "Collected"
        holder.submitted.text = "Submitted"
        holder.balance.text = "Balance"
        listOf(holder.date, holder.collected, holder.submitted, holder.balance).forEach {
            it.setTypeface(Typeface.DEFAULT_BOLD)
            it.setTextColor(it.context.getColor(R.color.textColorSecondary))
        }
        holder.itemView.visibility = if (isVisible) View.VISIBLE else View.GONE
        holder.itemView.layoutParams.height = if (isVisible) ViewGroup.LayoutParams.WRAP_CONTENT else 0
    }

    override fun getItemCount(): Int = 1

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val date: TextView = itemView.findViewById(R.id.tvDueDate)
        val collected: TextView = itemView.findViewById(R.id.tvDueCollected)
        val submitted: TextView = itemView.findViewById(R.id.tvDueSubmitted)
        val balance: TextView = itemView.findViewById(R.id.tvDueBalance)
    }
}

class DeliveryAgentDailyDueAdapter : ListAdapter<DeliveryAgentDailyDueRecord, DeliveryAgentDailyDueAdapter.ViewHolder>(DueDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.admin_delivery_agent_due_row, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val date: TextView = itemView.findViewById(R.id.tvDueDate)
        private val collected: TextView = itemView.findViewById(R.id.tvDueCollected)
        private val submitted: TextView = itemView.findViewById(R.id.tvDueSubmitted)
        private val balance: TextView = itemView.findViewById(R.id.tvDueBalance)

        fun bind(due: DeliveryAgentDailyDueRecord) {
            date.text = due.date
            collected.text = money(due.amountCollected)
            submitted.text = money(due.settledAmount)
            balance.text = money(due.balanceDue)
            balance.setTextColor(
                itemView.context.getColor(
                    if (due.balanceDue > 0.0) R.color.error_red else R.color.green_status
                )
            )
        }
    }

    class DueDiffCallback : DiffUtil.ItemCallback<DeliveryAgentDailyDueRecord>() {
        override fun areItemsTheSame(oldItem: DeliveryAgentDailyDueRecord, newItem: DeliveryAgentDailyDueRecord) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: DeliveryAgentDailyDueRecord, newItem: DeliveryAgentDailyDueRecord) = oldItem == newItem
    }
}

class DeliveryAgentItemAdapter : ListAdapter<DeliveryAgentItemSummary, DeliveryAgentItemAdapter.ViewHolder>(ItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.delivery_agent_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        private val tvItemMeta: TextView = itemView.findViewById(R.id.tvItemMeta)
        private val tvItemAmount: TextView = itemView.findViewById(R.id.tvItemAmount)

        fun bind(item: DeliveryAgentItemSummary) {
            val code = item.itemCode?.takeIf { it.isNotBlank() } ?: "-"
            tvItemName.text = item.itemName.orEmpty()
            tvItemMeta.text = "$code - Qty ${formatQty(item.quantity)} - ${formatCrates(item)} - ${item.billCount} bills"
            tvItemAmount.text = money(item.totalAmount)
        }
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<DeliveryAgentItemSummary>() {
        override fun areItemsTheSame(oldItem: DeliveryAgentItemSummary, newItem: DeliveryAgentItemSummary): Boolean = oldItem.itemId == newItem.itemId
        override fun areContentsTheSame(oldItem: DeliveryAgentItemSummary, newItem: DeliveryAgentItemSummary): Boolean = oldItem == newItem
    }
}

class DeliveryAgentBillAdapter(
    private val onClick: ((DeliveryAgentBillSummary) -> Unit)? = null
) : ListAdapter<DeliveryAgentBillSummary, DeliveryAgentBillAdapter.ViewHolder>(BillDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.admin_delivery_bill_detail, parent, false),
            onClick
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class ViewHolder(
        itemView: View,
        private val onClick: ((DeliveryAgentBillSummary) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvBillNumber: TextView = itemView.findViewById(R.id.tvBillNumber)
        private val tvBillDate: TextView = itemView.findViewById(R.id.tvBillDate)
        private val tvCustomerName: TextView = itemView.findViewById(R.id.tvCustomerName)
        private val tvBillAmount: TextView = itemView.findViewById(R.id.tvBillAmount)
        private val rvItems: RecyclerView = itemView.findViewById(R.id.rvItems)

        fun bind(bill: DeliveryAgentBillSummary) {
            tvBillNumber.text = bill.invoiceNumber ?: "#-"
            tvBillDate.text = bill.invoiceDate ?: "-"
            tvCustomerName.text = bill.customer ?: "Unknown Customer"
            tvBillAmount.text = money(bill.totalAmount)
            
            if (bill.items.isNotEmpty()) {
                rvItems.visibility = View.VISIBLE
                rvItems.layoutManager = LinearLayoutManager(itemView.context)
                rvItems.adapter = NestedBillItemAdapter(bill.items)
            } else {
                rvItems.visibility = View.GONE
            }
            
            itemView.setOnClickListener { onClick?.invoke(bill) }
        }
    }

    class BillDiffCallback : DiffUtil.ItemCallback<DeliveryAgentBillSummary>() {
        override fun areItemsTheSame(oldItem: DeliveryAgentBillSummary, newItem: DeliveryAgentBillSummary): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: DeliveryAgentBillSummary, newItem: DeliveryAgentBillSummary): Boolean = oldItem == newItem
    }
}

class NestedBillItemAdapter(private val items: List<DeliveryAgentBillItem>) : RecyclerView.Adapter<NestedBillItemAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.admin_bill_info_detail, parent, false))
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position], position + 1)
    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvIndex: TextView = itemView.findViewById(R.id.tvIndex)
        private val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        private val tvTotal: TextView = itemView.findViewById(R.id.tvTotal)

        fun bind(item: DeliveryAgentBillItem, index: Int) {
            tvIndex.text = index.toString()
            tvItemName.text = item.itemName
            tvQuantity.text = formatQty(item.quantity)
            tvTotal.text = money(item.amount)
        }
    }
}

private fun money(value: Double): String = "Rs. %.2f".format(Locale.US, value)

private fun formatQty(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(Locale.US, value)
}

private fun formatCrates(item: DeliveryAgentItemSummary): String {
    val piecesPerCrate = item.pcsCount
    if (piecesPerCrate <= 0) return "0 crates, ${formatQty(item.quantity)} pcs"

    val wholePieces = item.quantity.toInt()
    val crates = wholePieces / piecesPerCrate
    val remainingPieces = wholePieces % piecesPerCrate
    val fractionalPieces = item.quantity - wholePieces
    val remainingText = if (fractionalPieces == 0.0) {
        remainingPieces.toString()
    } else {
        formatQty(remainingPieces + fractionalPieces)
    }

    return "$crates crates, $remainingText pcs"
}
