package com.svd.svdagencies.ui.admin

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.customerData.UpdateBalanceRequest
import com.svd.svdagencies.data.model.delivery.DeliveryAgentBillSummary
import com.svd.svdagencies.data.model.delivery.DeliveryAgentDuesResponse
import com.svd.svdagencies.data.model.delivery.DeliveryAgentInfo
import com.svd.svdagencies.ui.admin.bills.AdminBillDetailActivity
import com.svd.svdagencies.ui.delivery.*
import com.svd.svdagencies.utils.AppSwipeRefreshLayout
import com.svd.svdagencies.utils.showLoading
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AdminDeliveryAgentDuesActivity : AdminBaseActivity() {
    private lateinit var swipeRefresh: AppSwipeRefreshLayout
    private lateinit var rvUnified: RecyclerView
    
    private lateinit var headerAdapter: DeliveryAgentSummaryHeaderAdapter
    private lateinit var duesHeaderAdapter: DeliveryAgentSectionHeaderAdapter
    private lateinit var duesTableHeaderAdapter: DeliveryAgentDuesTableHeaderAdapter
    private lateinit var duesAdapter: DeliveryAgentDailyDueAdapter
    private lateinit var itemsHeaderAdapter: DeliveryAgentSectionHeaderAdapter
    private lateinit var itemAdapter: DeliveryAgentItemAdapter
    private lateinit var billsHeaderAdapter: DeliveryAgentSectionHeaderAdapter
    private lateinit var billAdapter: DeliveryAgentBillAdapter

    private val startDate: Calendar = Calendar.getInstance()
    private val endDate: Calendar = Calendar.getInstance()
    private var agents: List<DeliveryAgentInfo> = emptyList()
    private var currentResponse: DeliveryAgentDuesResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_delivery_agent_dues)
        setupAdminLayout("Agent Dues")
        initDates()
        initViews()
        setupLists()
        setupListeners()
        loadReport()
    }

    override fun onResume() {
        super.onResume()
        loadReport()
    }

    private fun initDates() {
        startDate.set(Calendar.DAY_OF_MONTH, 1)
    }

    private fun initViews() {
        swipeRefresh = findViewById(R.id.swipeRefresh)
        rvUnified = findViewById(R.id.rvUnified)
    }

    private fun setupLists() {
        headerAdapter = DeliveryAgentSummaryHeaderAdapter(
            onClear = { clearFilters() },
            onApply = { btn -> loadReport(btn as? MaterialButton) },
            onPickStartDate = { pickDate(startDate) },
            onPickEndDate = { pickDate(endDate) },
            onRecordSubmission = { showSubmissionDialog() },
            onAgentSelected = { loadReport() }
        ).apply {
            startDateStr = apiDate(startDate)
            endDateStr = apiDate(endDate)
            selectedAgentName = "All Agents"
        }

        duesHeaderAdapter = DeliveryAgentSectionHeaderAdapter("Daily Agent Dues")
        duesTableHeaderAdapter = DeliveryAgentDuesTableHeaderAdapter()
        duesAdapter = DeliveryAgentDailyDueAdapter()
        itemsHeaderAdapter = DeliveryAgentSectionHeaderAdapter("Items Delivered")
        itemAdapter = DeliveryAgentItemAdapter()
        billsHeaderAdapter = DeliveryAgentSectionHeaderAdapter("Bill Details")
        billAdapter = DeliveryAgentBillAdapter { bill ->
            val intent = Intent(this, AdminBillDetailActivity::class.java).apply {
                putExtra("bill_id", bill.id)
            }
            startActivity(intent)
        }

        val concatAdapter = ConcatAdapter(
            headerAdapter,
            duesHeaderAdapter,
            duesTableHeaderAdapter,
            duesAdapter,
            itemsHeaderAdapter,
            itemAdapter,
            billsHeaderAdapter,
            billAdapter
        )
        rvUnified.apply {
            layoutManager = LinearLayoutManager(this@AdminDeliveryAgentDuesActivity)
            adapter = concatAdapter
        }
    }

    private fun setupListeners() {
        swipeRefresh.setOnRefreshListener { loadReport() }
    }

    private fun clearFilters() {
        startDate.time = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }.time
        endDate.time = Calendar.getInstance().time
        headerAdapter.selectedAgentName = "All Agents"
        updateDateLabels()
        loadReport()
    }

    private fun pickDate(calendar: Calendar) {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                updateDateLabels()
                loadReport()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadReport(btn: MaterialButton? = null) {
        swipeRefresh.isRefreshing = true
        btn?.showLoading(true, "Applying...")
        lifecycleScope.launch {
            try {
                val response = ApiClient.billsDashboardApi.getDeliveryAgentDues(
                    deliveryBoyId = selectedAgentId(),
                    startDate = apiDate(startDate),
                    endDate = apiDate(endDate)
                )
                render(response)
            } catch (e: Exception) {
                Toast.makeText(this@AdminDeliveryAgentDuesActivity, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh.isRefreshing = false
                btn?.showLoading(false)
            }
        }
    }

    private fun render(response: DeliveryAgentDuesResponse) {
        currentResponse = response
        agents = response.agents
        val names = listOf("All Agents") + agents.map { it.name.orEmpty() }
        
        headerAdapter.agentAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, names)
        headerAdapter.summary = response.summary
        headerAdapter.notifyItemChanged(0)

        duesHeaderAdapter.isVisible = response.dailyDues.isNotEmpty()
        duesHeaderAdapter.notifyItemChanged(0)
        duesTableHeaderAdapter.isVisible = response.dailyDues.isNotEmpty()
        duesTableHeaderAdapter.notifyItemChanged(0)
        duesAdapter.submitList(response.dailyDues)

        itemsHeaderAdapter.isVisible = response.items.isNotEmpty()
        itemsHeaderAdapter.notifyItemChanged(0)
        itemAdapter.submitList(response.items)
        
        val allBills = response.results.flatMap { it.bills }
        billsHeaderAdapter.isVisible = allBills.isNotEmpty()
        billsHeaderAdapter.notifyItemChanged(0)
        billAdapter.submitList(allBills)
    }

    private fun showSubmissionDialog() {
        val agentId = selectedAgentId()
        if (agentId == null) {
            Toast.makeText(this, "Select one agent to record submitted amount", Toast.LENGTH_SHORT).show()
            return
        }
        val agentName = headerAdapter.selectedAgentName
        val summary = currentResponse?.summary ?: return
        
        // Show total agent due as balance, and pre-fill amount with collected cash (holding amount)
        showUpdateBalanceDialog(agentId, agentName, summary.agentCurrentDue, summary.remainingGeneratedAmount)
    }

    private fun showUpdateBalanceDialog(customerId: Int, customerName: String, referenceBalance: Double, prefillAmount: Double? = null) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.admin_customer_balance_update)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val txtName = dialog.findViewById<TextView>(R.id.txtCustomerName)
        val txtBalance = dialog.findViewById<TextView>(R.id.txtCurrentBalance)
        val etAmount = dialog.findViewById<EditText>(R.id.etAmount)
        val btnUpdate = dialog.findViewById<MaterialButton>(R.id.btnUpdate)
        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnCancel)
        val btnClose = dialog.findViewById<ImageView>(R.id.btnClose)
        val chipAdd500 = dialog.findViewById<Chip>(R.id.chipAdd500)
        val chipAdd1000 = dialog.findViewById<Chip>(R.id.chipAdd1000)
        val chipClear = dialog.findViewById<Chip>(R.id.chipClear)

        txtName.text = customerName
        txtBalance.text = money(referenceBalance)
        
        if (prefillAmount != null && prefillAmount > 0) {
            etAmount.setText(String.format(Locale.US, "%.2f", prefillAmount))
            etAmount.setSelection(etAmount.text?.length ?: 0)
        }

        val dismissListener = View.OnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener(dismissListener)
        btnClose.setOnClickListener(dismissListener)

        fun adjustAmount(delta: Double) {
            val current = etAmount.text.toString().toDoubleOrNull() ?: 0.0
            val updated = current + delta
            etAmount.setText(String.format(Locale.US, "%.2f", updated))
            etAmount.setSelection(etAmount.text?.length ?: 0)
        }

        chipAdd500?.setOnClickListener { adjustAmount(500.0) }
        chipAdd1000?.setOnClickListener { adjustAmount(1000.0) }
        chipClear?.setOnClickListener { etAmount.text?.clear() }

        btnUpdate.setOnClickListener {
            val amountStr = etAmount.text.toString().trim()
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Please enter amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            updateCustomerBalance(customerId, amountStr, dialog)
        }

        dialog.show()
    }

    private fun updateCustomerBalance(id: Int, amount: String, dialog: Dialog) {
        val btnUpdate = dialog.findViewById<MaterialButton>(R.id.btnUpdate)
        btnUpdate?.showLoading(true, "Updating...")
        showScreenLoading()
        
        lifecycleScope.launch {
            try {
                val response = ApiClient.adminCustomerDashboard.updateBalance(id, UpdateBalanceRequest(amount = amount))
                if (response.success) {
                    Toast.makeText(this@AdminDeliveryAgentDuesActivity, "Balance updated", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadReport()
                } else {
                    Toast.makeText(this@AdminDeliveryAgentDuesActivity, response.message ?: "Failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminDeliveryAgentDuesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                btnUpdate?.showLoading(false)
                hideScreenLoading()
            }
        }
    }

    private fun selectedAgentId(): Int? {
        val selected = headerAdapter.selectedAgentName
        if (selected.isBlank() || selected == "All Agents") return null
        return agents.firstOrNull { it.name == selected }?.id
    }

    private fun updateDateLabels() {
        headerAdapter.startDateStr = apiDate(startDate)
        headerAdapter.endDateStr = apiDate(endDate)
        headerAdapter.notifyItemChanged(0)
    }

    private fun apiDate(calendar: Calendar): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }

    private fun money(value: Double): String = "₹%.2f".format(Locale.US, value)
}
