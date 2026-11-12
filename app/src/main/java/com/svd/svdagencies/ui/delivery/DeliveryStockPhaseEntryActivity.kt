package com.svd.svdagencies.ui.delivery

import com.svd.svdagencies.R
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.svd.svdagencies.base.BaseActivity
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.stock.StockItem
import com.svd.svdagencies.data.model.admin.customerData.CustomerItem
import com.svd.svdagencies.data.model.delivery.*
import com.svd.svdagencies.databinding.DeliveryStockPhaseEntryBinding
import com.svd.svdagencies.ui.delivery.adapter.DeliveryStockSingleInputAdapter
import com.svd.svdagencies.utils.RefreshManager
import com.svd.svdagencies.utils.SessionManager
import com.svd.svdagencies.utils.showLoading
import kotlinx.coroutines.launch
import retrofit2.awaitResponse
import java.util.Locale

class DeliveryStockPhaseEntryActivity : BaseActivity() {

    private lateinit var binding: DeliveryStockPhaseEntryBinding
    private lateinit var inputAdapter: DeliveryStockSingleInputAdapter
    private lateinit var session: SessionManager
    
    private var phase: Int = 0 // 0: Morning Stock, 1: Morning Return, 2: Evening Stock, 3: Evening Return
    private var selectedDate: String = ""
    private var adminStockEntry = false
    private var selectedAgentId: Int? = null
    private var deliveryAgents: List<CustomerItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DeliveryStockPhaseEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)

        phase = intent.getIntExtra("phase_type", 0)
        adminStockEntry = intent.getBooleanExtra("admin_stock_entry", false)
        selectedDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        
        binding.tvPhaseDate.text = "Date: ${selectedDate.split("-").reversed().joinToString("-")}"
        if (adminStockEntry) setupAdminAgentPicker() else loadItems()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        binding.toolbar.title = when (phase) {
            0 -> "Morning Entry"
            1 -> "Morning Return"
            2 -> "Evening Entry"
            3 -> "Evening Return"
            else -> "Stock Entry"
        }
    }

    private fun setupRecyclerView() {
        inputAdapter = DeliveryStockSingleInputAdapter(emptyList())
        binding.rvStockInput.apply {
            layoutManager = LinearLayoutManager(this@DeliveryStockPhaseEntryActivity)
            adapter = inputAdapter
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener { loadItems() }
        binding.btnSubmitPhase.setOnClickListener { submitPhase() }
    }

    private fun setupAdminAgentPicker() {
        val form = binding.rvStockInput.parent as? ViewGroup ?: return
        val picker = AutoCompleteTextView(this).apply {
            hint = "Select delivery agent"
            inputType = android.text.InputType.TYPE_NULL
            minimumHeight = (48 * resources.displayMetrics.density).toInt()
            setPadding((12 * resources.displayMetrics.density).toInt(), 0, (12 * resources.displayMetrics.density).toInt(), 0)
            background = getDrawable(R.drawable.bg_qty_outline)
            setOnClickListener { showDropDown() }
        }
        form.addView(picker, 1, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        lifecycleScope.launch {
            try {
                deliveryAgents = ApiClient.billsDashboardApi.getCustomersForBill("delivery").customers.orEmpty()
                    .filter { it.frozen != true }.sortedBy { it.name.orEmpty().lowercase(Locale.ROOT) }
                picker.setAdapter(ArrayAdapter(this@DeliveryStockPhaseEntryActivity, R.layout.delivery_dropdown_item,
                    listOf("Select delivery agent") + deliveryAgents.map { it.name.orEmpty() }))
                picker.setOnItemClickListener { _, _, position, _ ->
                    selectedAgentId = deliveryAgents.getOrNull(position - 1)?.id
                    if (selectedAgentId != null) loadItems()
                }
            } catch (_: Exception) {
                Toast.makeText(this@DeliveryStockPhaseEntryActivity, "Could not load delivery agents", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadItems() {
        val agentId = if (adminStockEntry) selectedAgentId else null
        if (adminStockEntry && agentId == null) {
            RefreshManager.stopRefresh(binding.swipeRefresh)
            return
        }
        RefreshManager.startRefresh(binding.swipeRefresh)
        lifecycleScope.launch {
            try {
                // Fetch allowed items from the specific phase endpoint
                val responseCall = when (phase) {
                    0 -> ApiClient.deliveryApi.getMorningStockItems(selectedDate, agentId)
                    1 -> ApiClient.deliveryApi.getMorningReturnItems(selectedDate, agentId)
                    2 -> ApiClient.deliveryApi.getEveningStockItems(selectedDate, agentId)
                    3 -> ApiClient.deliveryApi.getEveningReturnItems(selectedDate, agentId)
                    else -> ApiClient.deliveryApi.getMorningStockItems(selectedDate, agentId)
                }
                
                val response = responseCall.awaitResponse()
                
                if (response.isSuccessful) {
                    val rawItems = response.body()?.items ?: emptyList()
                    val items = rawItems.map { 
                        StockItem(
                            id = it.itemId,
                            name = it.name,
                            stockQuantity = it.quantity ?: 0.0,
                            sellingPrice = 0.0,
                            buyingPrice = 0.0,
                            companyName = null,
                            pcsCount = it.pcsCount,
                            categoryName = it.categoryName,
                            itemCode = it.code,
                            image = it.image
                        )
                    }
                    inputAdapter = DeliveryStockSingleInputAdapter(sortStockItems(items))
                    binding.rvStockInput.adapter = inputAdapter

                    // Update button text based on existing data
                    val hasExistingData = rawItems.any { (it.quantity ?: 0.0) > 0.0 }
                    binding.btnSubmitPhase.text = if (hasExistingData) "Update Stock" else "Save Stock"
                } else {
                    Toast.makeText(this@DeliveryStockPhaseEntryActivity, "Failed to load items", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DeliveryStockPhaseEntryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                RefreshManager.stopRefresh(binding.swipeRefresh)
            }
        }
    }

    private fun submitPhase() {
        if (adminStockEntry && selectedAgentId == null) {
            Toast.makeText(this, "Select a delivery agent first", Toast.LENGTH_SHORT).show()
            return
        }
        val quantities = inputAdapter.getQuantities()
        if (quantities.isEmpty()) {
            Toast.makeText(this, "No data entered", Toast.LENGTH_SHORT).show()
            return
        }

        val items = quantities.entries.map { StockEntryItemInput(it.key, it.value) }
        val request = DeliveryStockEntryRequest(
            deliveryAgent = selectedAgentId ?: session.getUserId(),
            date = selectedDate,
            items = items
        )

        binding.btnSubmitPhase.showLoading(true, "Saving...")

        lifecycleScope.launch {
            try {
                val apiCall = when (phase) {
                    0 -> ApiClient.deliveryApi.submitMorningStock(request)
                    1 -> ApiClient.deliveryApi.submitMorningReturn(request)
                    2 -> ApiClient.deliveryApi.submitEveningStock(request)
                    3 -> ApiClient.deliveryApi.submitEveningReturn(request)
                    else -> null
                }

                val response = apiCall?.awaitResponse()
                if (response?.isSuccessful == true) {
                    Toast.makeText(this@DeliveryStockPhaseEntryActivity, "Data saved successfully", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@DeliveryStockPhaseEntryActivity, "Save failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DeliveryStockPhaseEntryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnSubmitPhase.showLoading(false)
            }
        }
    }

    private fun sortStockItems(items: List<StockItem>): List<StockItem> {
        val categoryOrder = listOf("milk", "curd", "buckets", "cups", "ghee", "flavoured milk", "paneer", "bread", "drinks")
        return items.sortedWith(compareBy<StockItem> {
            val cat = it.categoryName?.lowercase() ?: ""
            val name = it.name.lowercase()
            
            var index = categoryOrder.indexOf(cat)
            if (index == -1) {
                index = when {
                    name.contains("milk", ignoreCase = true) -> 0
                    name.contains("curd", ignoreCase = true) -> 1
                    else -> categoryOrder.size
                }
            }
            index
        }.thenBy { it.name })
    }
}
