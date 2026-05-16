package com.svd.svdagencies.ui.delivery

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.svd.svdagencies.R
import com.svd.svdagencies.base.BaseActivity
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.delivery.DeliveryAgentDuesResponse
import com.svd.svdagencies.data.model.delivery.DeliveryTodayBill
import com.svd.svdagencies.databinding.DeliveryDashboardBinding
import com.svd.svdagencies.ui.auth.LoginActivity
import com.svd.svdagencies.utils.SessionManager
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.awaitResponse
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DeliveryDashboardActivity : BaseActivity() {

    private lateinit var binding: DeliveryDashboardBinding
    private lateinit var todayBillAdapter: DeliveryTodayBillAdapter
    private lateinit var agentItemAdapter: DeliveryAgentItemAdapter
    private lateinit var sessionManager: SessionManager
    private var selectedDate = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DeliveryDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sessionManager = SessionManager(this)

        setupToolbar(binding.toolbar, "Report")
        DeliveryNavigation.setup(
            this,
            binding.deliveryDrawerLayout,
            binding.deliveryNavigationView,
            menuButton = binding.toolbar.findViewById(R.id.btnMenu),
            selectedItemId = R.id.nav_delivery_today_report
        )
        setupRecyclerViews()
        setupSwipeRefresh()
        setupDatePicker()
        
        // refreshData() removed from onCreate as it is called in onResume
    }

    private fun setupDatePicker() {
        updateDateDisplay()
        binding.layoutDatePicker.setOnClickListener {
            val datePickerDialog = android.app.DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedDate.set(Calendar.YEAR, year)
                    selectedDate.set(Calendar.MONTH, month)
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    updateDateDisplay()
                    refreshData()
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }
    }

    private fun updateDateDisplay() {
        binding.tvSelectedDate.text = apiDate(selectedDate)
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    private fun setupToolbar(toolbar: Toolbar, title: String) {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.findViewById<TextView>(R.id.tvToolbarTitle)?.text = title
    }

    private fun setupRecyclerViews() {
        // Today's Bills Adapter
        todayBillAdapter = DeliveryTodayBillAdapter(
            onViewBill = { bill -> showBillDetails(bill) },
            onDeleteBill = { bill -> confirmDeleteBill(bill) }
        )
        binding.rvTodayBills.apply {
            layoutManager = LinearLayoutManager(this@DeliveryDashboardActivity)
            adapter = todayBillAdapter
            isNestedScrollingEnabled = false
        }

        // Agent Report Adapters (Monthly)
        agentItemAdapter = DeliveryAgentItemAdapter()
        binding.rvAgentItems.apply {
            layoutManager = LinearLayoutManager(this@DeliveryDashboardActivity)
            adapter = agentItemAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            refreshData()
        }
    }

    private fun refreshData() {
        loadTodayBills()
    }

    private fun loadTodayBills() {
        binding.swipeRefresh.isRefreshing = true
        val dateStr = apiDate(selectedDate)
        
        ApiClient.deliveryApi.getAgentSummary(dateStr).enqueue(object : Callback<DeliveryAgentDuesResponse> {
            override fun onResponse(call: Call<DeliveryAgentDuesResponse>, response: Response<DeliveryAgentDuesResponse>) {
                binding.swipeRefresh.isRefreshing = false
                if (response.isSuccessful) {
                    val body = response.body()
                    val bills = body?.results?.firstOrNull()?.bills ?: emptyList()

                    // Convert to DeliveryTodayBill for the adapter
                    val todayBills = bills.map { 
                        DeliveryTodayBill(
                            id = it.id,
                            billId = it.id,
                            billNumber = it.invoiceNumber,
                            totalAmount = it.totalAmount,
                            date = it.invoiceDate,
                            fileUrl = null
                        )
                    }

                    todayBillAdapter.submitList(todayBills)
                    
                    binding.rvTodayBills.visibility = if (todayBills.isEmpty()) View.GONE else View.VISIBLE
                    binding.layoutEmpty.visibility = if (todayBills.isEmpty()) View.VISIBLE else View.GONE
                    
                    // Also update agent statistics for this specific date
                    body?.let { renderAgentSummary(it) }
                }
            }

            override fun onFailure(call: Call<DeliveryAgentDuesResponse>, t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(this@DeliveryDashboardActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun renderAgentSummary(response: DeliveryAgentDuesResponse) {
        val summary = response.summary
        val dateStr = apiDate(selectedDate)
        val todayStr = apiDate(Calendar.getInstance())
        
        binding.tvAgentDuesTitle.text = if (dateStr == todayStr) {
            "${response.agent?.name ?: "Your"} Today Dues"
        } else {
            "${response.agent?.name ?: "Your"} Dues for $dateStr"
        }

        binding.tvAgentBillCount.text = "Bills\n${summary.billCount}"
        binding.tvAgentDeliveredAmount.text = "Total Sale\n${money(summary.totalAmount)}"
        val submittedAmount = submittedAmount(summary)
        val remainingAmount = when {
            summary.remainingGeneratedAmount > 0.0 -> summary.remainingGeneratedAmount
            else -> (summary.totalAmount - submittedAmount).coerceAtLeast(0.0)
        }
        binding.tvAgentCounterDue.text = "Submitted\n${money(submittedAmount)}"
        binding.tvAgentCollectedAmount.text = "Collected\n${money(submittedAmount)}"
        binding.tvAgentRemainingAmount.text = "Remaining\n${money(remainingAmount)}"
        binding.tvAgentSelfBillAmount.text = "Bill Generator\n${money(summary.selfBillAmount)}"
        binding.tvAgentProfit.text = "Profit\n${money(summary.totalProfit)}"
        agentItemAdapter.submitList(response.items)
    }

    private fun submittedAmount(summary: com.svd.svdagencies.data.model.delivery.DeliveryAgentDuesSummary): Double {
        return when {
            summary.submittedAmount > 0.0 -> summary.submittedAmount
            summary.collectedAmount > 0.0 -> summary.collectedAmount
            summary.counterSubmitAmount > 0.0 -> summary.counterSubmitAmount
            else -> summary.totalPaid
        }
    }

    private fun showBillDetails(bill: DeliveryTodayBill) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_bill_details, null)
        val tvBillNumber = dialogView.findViewById<TextView>(R.id.tvDialogBillNumber)
        val tvTotalAmount = dialogView.findViewById<TextView>(R.id.tvDialogTotalAmount)
        val rvItems = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvBillDetailItems)
        val btnClose = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCloseDialog)

        tvBillNumber.text = bill.billNumber ?: "#${bill.realId}"
        tvTotalAmount.text = money(bill.totalAmount)

        val detailAdapter = BillDetailItemAdapter()
        rvItems.layoutManager = LinearLayoutManager(this)
        rvItems.adapter = detailAdapter

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()

        lifecycleScope.launch {
            try {
                // First, fetch the bill summary details
                val response = ApiClient.deliveryApi.getBillDetails(bill.realId).awaitResponse()
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let { res ->
                        tvTotalAmount.text = money(res.total_amount)
                    }
                }

                // Second, fetch the items list from the separate endpoint identified in logs
                val itemsResponse = ApiClient.deliveryApi.getBillItemsDetail(bill.realId).awaitResponse()
                if (itemsResponse.isSuccessful) {
                    val items = itemsResponse.body()
                    items?.let { itemsList ->
                        val convertedItems = itemsList.map { 
                            com.svd.svdagencies.data.model.admin.Bills.BillItemDetail(
                                item_id = it.itemId,
                                item_name = it.name,
                                quantity = it.quantity,
                                price_per_unit = it.pricePerUnit,
                                discount = it.discount,
                                total_discount = it.totalDiscount,
                                total_amount = it.totalAmount
                            )
                        }
                        detailAdapter.submitList(convertedItems)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@DeliveryDashboardActivity, "Error loading details", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDeleteBill(bill: DeliveryTodayBill) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Bill")
            .setMessage("Are you sure you want to delete bill ${bill.billNumber ?: "#${bill.realId}"}?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                deleteBill(bill)
            }
            .show()
    }

    private fun deleteBill(bill: DeliveryTodayBill) {
        lifecycleScope.launch {
            try {
                ApiClient.billsDashboardApi.deleteBill(bill.realId)
                Toast.makeText(this@DeliveryDashboardActivity, "Bill deleted", Toast.LENGTH_SHORT).show()
                refreshData()
            } catch (e: Exception) {
                Toast.makeText(this@DeliveryDashboardActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun apiDate(calendar: Calendar): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }

    private fun money(value: Double): String = "₹%.2f".format(Locale.US, value)
}
