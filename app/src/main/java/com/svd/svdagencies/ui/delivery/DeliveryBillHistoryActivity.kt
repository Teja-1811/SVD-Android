package com.svd.svdagencies.ui.delivery

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.svd.svdagencies.R
import com.svd.svdagencies.base.BaseActivity
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.delivery.DeliveryTodayBill
import com.svd.svdagencies.utils.SessionManager
import kotlinx.coroutines.launch
import retrofit2.awaitResponse
import java.util.Locale

class DeliveryBillHistoryActivity : BaseActivity() {

    private var customerId: Int = 0
    private lateinit var rvTodayBills: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var layoutEmpty: View
    private lateinit var cardSummary: View
    private lateinit var tvTotalBillsCount: TextView
    private lateinit var tvTotalInvoiceAmount: TextView
    private lateinit var todayBillAdapter: DeliveryTodayBillAdapter
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.delivery_bill_history)

        sessionManager = SessionManager(this)
        customerId = intent.getIntExtra("customer_id", 0)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = "Today's Bills"
        toolbar.setNavigationOnClickListener { finish() }

        rvTodayBills = findViewById(R.id.rvTodayBills)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        cardSummary = findViewById(R.id.cardSummary)
        tvTotalBillsCount = findViewById(R.id.tvTotalBillsCount)
        tvTotalInvoiceAmount = findViewById(R.id.tvTotalInvoiceAmount)

        setupRecyclerView()
        setupListeners()
        fetchBills()
    }

    private fun setupRecyclerView() {
        todayBillAdapter = DeliveryTodayBillAdapter(
            onViewBill = { bill -> showBillDetails(bill) },
            onDeleteBill = { bill -> confirmDeleteBill(bill) }
        )
        rvTodayBills.layoutManager = LinearLayoutManager(this)
        rvTodayBills.adapter = todayBillAdapter
    }

    private fun setupListeners() {
        swipeRefresh.setOnRefreshListener { fetchBills() }
    }

    private fun fetchBills() {
        val targetId = if (customerId > 0) customerId else sessionManager.getUserId()
        if (targetId <= 0) return

        lifecycleScope.launch {
            swipeRefresh.isRefreshing = true
            try {
                val response = ApiClient.deliveryApi.getTodayBills(targetId).awaitResponse()
                if (response.isSuccessful) {
                    val body = response.body()
                    val bills = body?.bills ?: emptyList()
                    val totalAmount = body?.totalInvoiceAmount ?: 0.0

                    todayBillAdapter.submitList(bills)
                    layoutEmpty.visibility = if (bills.isEmpty()) View.VISIBLE else View.GONE
                    
                    if (bills.isNotEmpty()) {
                        cardSummary.visibility = View.VISIBLE
                        tvTotalBillsCount.text = "${bills.size} Bills"
                        tvTotalInvoiceAmount.text = money(totalAmount)
                    } else {
                        cardSummary.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(this@DeliveryBillHistoryActivity, "Failed to load bills", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DeliveryBillHistoryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showBillDetails(bill: DeliveryTodayBill) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_bill_details, null)
        val tvBillNumber = dialogView.findViewById<TextView>(R.id.tvDialogBillNumber)
        val tvTotalAmount = dialogView.findViewById<TextView>(R.id.tvDialogTotalAmount)
        val rvItems = dialogView.findViewById<RecyclerView>(R.id.rvBillDetailItems)
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
                val response = ApiClient.deliveryApi.getBillDetails(bill.realId).awaitResponse()
                if (response.isSuccessful) {
                    val detail = response.body()
                    detail?.let { res ->
                        tvTotalAmount.text = money(res.total_amount)
                        res.items?.let { items ->
                            val convertedItems = items.map { 
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
                        } ?: run {
                            try {
                                val itemResponse = ApiClient.billsDashboardApi.getBillItems(bill.realId)
                                detailAdapter.submitList(itemResponse)
                            } catch (e: Exception) {
                                Toast.makeText(this@DeliveryBillHistoryActivity, "Unable to load item breakdown", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@DeliveryBillHistoryActivity, "Error loading details", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this@DeliveryBillHistoryActivity, "Bill deleted", Toast.LENGTH_SHORT).show()
                fetchBills()
            } catch (e: Exception) {
                Toast.makeText(this@DeliveryBillHistoryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun money(value: Double): String = "₹ %.2f".format(Locale.US, value)
}
