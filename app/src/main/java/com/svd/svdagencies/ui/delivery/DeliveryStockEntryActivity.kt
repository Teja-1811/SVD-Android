package com.svd.svdagencies.ui.delivery

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.svd.svdagencies.R
import com.svd.svdagencies.base.BaseActivity
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.delivery.*
import com.svd.svdagencies.databinding.ActivityDeliveryStockEntryBinding
import com.svd.svdagencies.ui.delivery.adapter.DeliveryStockReportAdapter
import com.svd.svdagencies.utils.NetworkMessageUtils
import com.svd.svdagencies.utils.RefreshManager
import com.svd.svdagencies.utils.SessionManager
import kotlinx.coroutines.launch
import retrofit2.awaitResponse
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DeliveryStockEntryActivity : BaseActivity() {

    private lateinit var binding: ActivityDeliveryStockEntryBinding
    private val reportAdapter = DeliveryStockReportAdapter()
    
    private var selectedDate = Calendar.getInstance()
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.US)
    private lateinit var session: SessionManager

    private val phaseLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            fetchReconciliationReport()
        }
    }

    private val historyLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val dateStr = result.data?.getStringExtra("selected_date")
            dateStr?.let {
                try {
                    val date = apiDateFormat.parse(it)
                    selectedDate.time = date!!
                    updateDateDisplay()
                    fetchReconciliationReport()
                } catch (e: Exception) {}
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeliveryStockEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)

        setupToolbar()
        setupListeners()
        setupRecyclerViews()
        
        updateDateDisplay()
        fetchReconciliationReport()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        binding.toolbar.findViewById<View>(R.id.btnMenu)?.setOnClickListener {
            binding.deliveryDrawerLayout.openDrawer(androidx.core.view.GravityCompat.START)
        }
        
        DeliveryNavigation.setup(
            this,
            binding.deliveryDrawerLayout,
            binding.deliveryNavigationView,
            menuButton = binding.toolbar.findViewById(R.id.btnMenu),
            selectedItemId = R.id.nav_delivery_stock_entry
        )
    }

    private fun setupListeners() {
        binding.toolbar.findViewById<View>(R.id.layoutDatePickerToolbar)?.setOnClickListener { showDatePicker() }
        binding.btnRefreshReport.setOnClickListener { fetchReconciliationReport() }
        binding.btnViewHistory.setOnClickListener { 
            historyLauncher.launch(Intent(this, DeliveryStockHistoryActivity::class.java))
        }
        binding.swipeRefresh.setOnRefreshListener { 
            fetchReconciliationReport()
        }

        binding.btnMorningStock.setOnClickListener { openPhaseEntry(0) }
        binding.btnMorningReturn.setOnClickListener { openPhaseEntry(1) }
        binding.btnEveningStock.setOnClickListener { openPhaseEntry(2) }
        binding.btnEveningReturn.setOnClickListener { openPhaseEntry(3) }
        
        binding.btnGenerateBill.setOnClickListener {
            prepareBillConfirmation()
        }
    }

    private fun prepareBillConfirmation() {
        val remainingItems = reportAdapter.getItems().filter { it.difference > 0 }
        if (remainingItems.isEmpty()) {
            Toast.makeText(this, "No remaining stock to bill", Toast.LENGTH_SHORT).show()
            return
        }

        showScreenLoading()
        lifecycleScope.launch {
            try {
                val response = ApiClient.deliveryApi.getBillItems(session.getUserId()).awaitResponse()
                if (response.isSuccessful) {
                    val catalog = response.body()?.items ?: emptyList()
                    val billingItems = mutableListOf<Pair<DeliveryBillItem, Int>>()
                    
                    remainingItems.forEach { stockItem ->
                        val catalogItem = catalog.find { it.itemId == stockItem.itemId }
                        if (catalogItem != null) {
                            billingItems.add(catalogItem to stockItem.difference.toInt())
                        }
                    }

                    if (billingItems.isNotEmpty()) {
                        showConfirmationDialog(billingItems)
                    } else {
                        Toast.makeText(this@DeliveryStockEntryActivity, "Billing data not found for items", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@DeliveryStockEntryActivity, "Failed to load billing prices", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DeliveryStockEntryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                hideScreenLoading()
            }
        }
    }

    private fun showConfirmationDialog(selectedItems: List<Pair<DeliveryBillItem, Int>>) {
        val dialogView = layoutInflater.inflate(R.layout.delivery_bill_confirmation, null)
        val rvConfirmItems = dialogView.findViewById<RecyclerView>(R.id.rvConfirmItems)
        val tvConfirmCustomer = dialogView.findViewById<TextView>(R.id.tvConfirmCustomer)
        val tvConfirmTotal = dialogView.findViewById<TextView>(R.id.tvConfirmTotal)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btnConfirm)
        val btnUpi = dialogView.findViewById<MaterialButton>(R.id.btnUpi)

        var totalAmount = 0.0
        selectedItems.forEach { (item, qty) ->
            totalAmount += item.price * qty
        }

        tvConfirmCustomer.text = "Customer: Self"
        tvConfirmTotal.text = "₹ %.2f".format(totalAmount)

        rvConfirmItems.layoutManager = LinearLayoutManager(this)
        rvConfirmItems.adapter = DeliveryBillConfirmationAdapter(selectedItems)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            processBillGeneration(selectedItems, totalAmount)
        }

        btnUpi.visibility = View.GONE // Keep it simple for self-entry

        dialog.show()
    }

    private fun processBillGeneration(selectedItems: List<Pair<DeliveryBillItem, Int>>, total: Double) {
        showScreenLoading()
        lifecycleScope.launch {
            try {
                val request = DeliveryGenerateBillRequest(
                    customerId = session.getUserId(),
                    billDate = apiDateFormat.format(Calendar.getInstance().time),
                    items = selectedItems.map { BillLineItem(it.first.itemId, it.second, 0.0) },
                    paidAmount = 0.0,
                    billMode = "regular"
                )

                val response = ApiClient.deliveryApi.generateBill(request).awaitResponse()
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@DeliveryStockEntryActivity, "Bill Generated Successfully", Toast.LENGTH_SHORT).show()
                    fetchReconciliationReport()
                } else {
                    val msg = NetworkMessageUtils.parseError(response, response.body()?.message ?: "Failed")
                    Toast.makeText(this@DeliveryStockEntryActivity, msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DeliveryStockEntryActivity, "Error generating bill", Toast.LENGTH_SHORT).show()
            } finally {
                hideScreenLoading()
            }
        }
    }

    private fun openPhaseEntry(phaseType: Int) {
        val intent = Intent(this, DeliveryStockPhaseEntryActivity::class.java).apply {
            putExtra("phase_type", phaseType)
            putExtra("selected_date", apiDateFormat.format(selectedDate.time))
        }
        phaseLauncher.launch(intent)
    }

    private fun setupRecyclerViews() {
        binding.rvStockReport.apply {
            layoutManager = LinearLayoutManager(this@DeliveryStockEntryActivity)
            adapter = reportAdapter
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, month)
                selectedDate.set(Calendar.DAY_OF_MONTH, day)
                updateDateDisplay()
                fetchReconciliationReport()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateDisplay() {
        binding.tvSelectedDate.text = displayDateFormat.format(selectedDate.time)
    }

    private fun fetchReconciliationReport() {
        RefreshManager.startRefresh(binding.swipeRefresh)
        lifecycleScope.launch {
            try {
                val response = ApiClient.deliveryApi.getDeliveryDashboard(
                    agentId = session.getUserId(),
                    date = apiDateFormat.format(selectedDate.time)
                ).awaitResponse()

                if (response.isSuccessful) {
                    val data = response.body()
                    if (data?.success == true) {
                        renderReport(data)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("StockEntry", "Report Error", e)
            } finally {
                RefreshManager.stopRefresh(binding.swipeRefresh)
            }
        }
    }

    private fun renderReport(response: DeliveryDashboardReportResponse) {
        binding.layoutReport.visibility = View.VISIBLE
        reportAdapter.submitList(response.items)
        
        // Update summary card
        val summary = response.summary
        binding.tvTotalCollected.text = formatQty(summary.totalStock)
        binding.tvTotalReturned.text = formatQty(summary.totalReturn)
        binding.tvNetSold.text = formatQty(summary.netStock)
        binding.tvTotalBilled.text = formatQty(summary.totalBilled)
        binding.tvTotalDifference.text = formatQty(summary.difference)

        if (summary.difference > 0) {
            binding.tvTotalDifference.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.brand_red))
        } else {
            binding.tvTotalDifference.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.icon_green))
        }

        binding.layoutReport.post {
            binding.nestedScrollView.smoothScrollTo(0, binding.layoutReport.top)
        }
    }

    private fun formatQty(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)
    }
}
