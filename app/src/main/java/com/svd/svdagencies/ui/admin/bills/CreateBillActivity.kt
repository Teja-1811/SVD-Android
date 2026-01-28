package com.svd.svdagencies.ui.admin.bills

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.AdminItem
import com.svd.svdagencies.data.model.admin.Bills.BillCustomer
import com.svd.svdagencies.data.model.admin.BillItemForCreation
import com.svd.svdagencies.data.model.admin.CreateBillRequest
import com.svd.svdagencies.data.model.admin.EditBillRequest
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateBillActivity : AdminBaseActivity() {

    private var billId: Int? = null

    private lateinit var spinnerArea: Spinner
    private lateinit var spinnerCustomer: Spinner
    private lateinit var tvBillDate: TextView
    private lateinit var etSearchItems: EditText
    private lateinit var spinnerItem: Spinner
    private lateinit var etQty: EditText
    private lateinit var etDiscount: EditText
    private lateinit var btnAddItem: Button
    private lateinit var rvBillItems: RecyclerView
    private lateinit var btnGenerateBill: Button

    private lateinit var itemAdapter: CreateBillItemAdapter
    private var allCustomers: List<BillCustomer> = emptyList()
    private var filteredCustomers: List<BillCustomer> = emptyList()
    private var allAvailableItems: List<AdminItem> = emptyList()
    private var filteredItems: List<AdminItem> = emptyList()

    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_bill_create)
        
        billId = intent.getIntExtra("bill_id", -1).takeIf { it != -1 }

        setupAdminLayout(if (billId == null) "Create Bill" else "Edit Bill")

        initViews()
        setupRecyclerView()
        setupListeners()

        updateDateDisplay()
        fetchInitialData()
    }

    private fun initViews() {
        spinnerArea = findViewById(R.id.spinnerArea)
        spinnerCustomer = findViewById(R.id.spinnerCustomer)
        tvBillDate = findViewById(R.id.tvBillDate)
        etSearchItems = findViewById(R.id.etSearchItems)
        spinnerItem = findViewById(R.id.spinnerItem)
        etQty = findViewById(R.id.etQty)
        etDiscount = findViewById(R.id.etDiscount)
        btnAddItem = findViewById(R.id.btnAddItem)
        rvBillItems = findViewById(R.id.rvBillItems)
        btnGenerateBill = findViewById(R.id.btnGenerateBill)
    }

    private fun setupRecyclerView() {
        itemAdapter = CreateBillItemAdapter(mutableListOf())
        rvBillItems.layoutManager = LinearLayoutManager(this)
        rvBillItems.adapter = itemAdapter
    }

    private fun setupListeners() {
        tvBillDate.setOnClickListener { showDatePicker() }

        spinnerArea.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterCustomers()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        etSearchItems.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterItems(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnAddItem.setOnClickListener { 
            val selectedPos = spinnerItem.selectedItemPosition
            if (selectedPos >= 0 && filteredItems.isNotEmpty()) {
                val item = filteredItems[selectedPos]
                val qty = etQty.text.toString().toIntOrNull() ?: 1
                val disc = etDiscount.text.toString().toDoubleOrNull() ?: 0.0
                
                itemAdapter.addItem(BillItemForCreation(item.id, qty, disc))
                
                // Clear inputs
                etQty.setText("")
                etDiscount.setText("")
            } else {
                Toast.makeText(this, "Please select an item", Toast.LENGTH_SHORT).show()
            }
        }

        btnGenerateBill.setOnClickListener { saveBill() }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                updateDateDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateDisplay() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        tvBillDate.text = sdf.format(calendar.time)
    }

    private fun fetchInitialData() {
        lifecycleScope.launch {
            try {
                val customersDeferred = async { ApiClient.billsDashboardApi.getCustomersForBill() }
                val itemsDeferred = async { fetchAllAvailableItems() }
                
                allCustomers = customersDeferred.await()
                allAvailableItems = itemsDeferred.await()

                // Setup Area Spinner
                val areas = listOf("All Areas") + allCustomers.map { it.area }.distinct()
                val areaAdapter = ArrayAdapter(this@CreateBillActivity, android.R.layout.simple_spinner_item, areas)
                areaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerArea.adapter = areaAdapter

                // Initial Item Filter
                filterItems("")
                itemAdapter.updateAvailableItems(allAvailableItems)

                billId?.let { id ->
                    val billDetail = ApiClient.billsDashboardApi.getBillDetail(id)
                    val billItems = ApiClient.billsDashboardApi.getBillItems(id)
                    
                    // Match customer
                    val customer = allCustomers.find { it.name == billDetail.customer }
                    customer?.let {
                        val areaPos = areas.indexOf(it.area)
                        if (areaPos != -1) spinnerArea.setSelection(areaPos)
                        
                        // Wait for spinnerArea update to trigger filterCustomers then set spinnerCustomer
                        spinnerCustomer.post {
                            val custPos = filteredCustomers.indexOfFirst { c -> c.id == it.id }
                            if (custPos != -1) spinnerCustomer.setSelection(custPos)
                        }
                    }
                    
                    val itemsForCreation = billItems.map { 
                        BillItemForCreation(
                            itemId = it.item_id,
                            quantity = it.quantity,
                            discount = it.discount
                        )
                    }
                    
                    itemAdapter = CreateBillItemAdapter(itemsForCreation.toMutableList(), allAvailableItems)
                    rvBillItems.adapter = itemAdapter
                }

            } catch (e: Exception) {
                Toast.makeText(this@CreateBillActivity, "Error fetching data: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun filterCustomers() {
        val selectedArea = spinnerArea.selectedItem.toString()
        filteredCustomers = if (selectedArea == "All Areas") {
            allCustomers
        } else {
            allCustomers.filter { it.area == selectedArea }
        }

        val names = filteredCustomers.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCustomer.adapter = adapter
    }

    private fun filterItems(query: String) {
        filteredItems = if (query.isEmpty()) {
            allAvailableItems
        } else {
            allAvailableItems.filter { it.name.contains(query, ignoreCase = true) }
        }

        val names = filteredItems.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerItem.adapter = adapter
    }

    private suspend fun fetchAllAvailableItems(): List<AdminItem> {
        val categories = ApiClient.adminItemsApi.getCategories()
        return categories.map { category ->
            lifecycleScope.async { ApiClient.adminItemsApi.getItemsByCategory(category) }
        }.awaitAll().flatten().distinctBy { it.id }
    }

    private fun saveBill() {
        lifecycleScope.launch {
            try {
                if (billId == null) createBill() else editBill(billId!!)
            } catch (e: Exception) {
                Toast.makeText(this@CreateBillActivity, "Error saving bill: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun createBill() {
        val custPos = spinnerCustomer.selectedItemPosition
        if (custPos < 0) {
            Toast.makeText(this, "Please select a customer", Toast.LENGTH_SHORT).show()
            return
        }
        val customerId = filteredCustomers[custPos].id

        val billItems = itemAdapter.getItems().filter { it.itemId != 0 }
        if (billItems.isEmpty()) {
            Toast.makeText(this, "Please add at least one item", Toast.LENGTH_SHORT).show()
            return
        }

        val request = CreateBillRequest(
            customerId,
            billItems.map { it.itemId },
            billItems.map { it.quantity },
            billItems.map { it.discount }
        )
        ApiClient.billsDashboardApi.createBill(request)
        Toast.makeText(this, "Bill created successfully!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private suspend fun editBill(id: Int) {
        val billItems = itemAdapter.getItems().filter { it.itemId != 0 }
        if (billItems.isEmpty()) {
            Toast.makeText(this, "Please add at least one item", Toast.LENGTH_SHORT).show()
            return
        }

        val request = EditBillRequest(
            billItems.map { it.itemId },
            billItems.map { it.quantity },
            billItems.map { it.discount }
        )
        ApiClient.billsDashboardApi.editBill(id, request)
        Toast.makeText(this, "Bill updated successfully!", Toast.LENGTH_SHORT).show()
        finish()
    }
}
