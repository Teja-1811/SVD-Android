package com.svd.svdagencies.ui.admin.bills

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.Items.AdminItem
import com.svd.svdagencies.data.model.admin.customerData.CustomerItem
import com.svd.svdagencies.data.model.admin.Bills.BillItemForCreation
import com.svd.svdagencies.data.model.admin.Bills.CreateBillRequest
import com.svd.svdagencies.data.model.admin.Bills.EditBillRequest
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import com.svd.svdagencies.utils.NetworkMessageUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlin.math.ceil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateBillActivity : AdminBaseActivity() {

    private var billId: Int? = null

    private lateinit var spinnerArea: AutoCompleteTextView
    private lateinit var spinnerCustomer: AutoCompleteTextView
    private lateinit var tvBillDate: TextInputEditText
    private lateinit var etSearchItems: TextInputEditText
    private lateinit var spinnerItem: AutoCompleteTextView
    private lateinit var etAvailableQty: TextInputEditText
    private lateinit var etQty: TextInputEditText
    private lateinit var etDiscount: TextInputEditText
    private lateinit var btnAddItem: MaterialButton
    private lateinit var rvBillItems: RecyclerView
    private lateinit var btnGenerateBill: MaterialButton
    
    private lateinit var tvTotalQty: TextView
    private lateinit var tvTotalDiscAmount: TextView
    private lateinit var tvGrandTotal: TextView

    private lateinit var itemAdapter: CreateBillItemAdapter
    private var allCustomers: List<CustomerItem> = emptyList()
    private var filteredCustomers: List<CustomerItem> = emptyList()
    private var allAvailableItems: List<AdminItem> = emptyList()
    private var filteredItems: List<AdminItem> = emptyList()
    
    private var selectedCustomer: CustomerItem? = null
    private var selectedItem: AdminItem? = null

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
        etAvailableQty = findViewById(R.id.etAvailableQty)
        etQty = findViewById(R.id.etQty)
        etDiscount = findViewById(R.id.etDiscount)
        btnAddItem = findViewById(R.id.btnAddItem)
        rvBillItems = findViewById(R.id.rvBillItems)
        btnGenerateBill = findViewById(R.id.btnGenerateBill)
        
        tvTotalQty = findViewById(R.id.tvTotalQty)
        tvTotalDiscAmount = findViewById(R.id.tvTotalDiscAmount)
        tvGrandTotal = findViewById(R.id.tvGrandTotal)
    }

    private fun setupRecyclerView() {
        itemAdapter = CreateBillItemAdapter(mutableListOf(), onRemoveClick = { position ->
            itemAdapter.removeItem(position)
            updateSummary()
        })
        rvBillItems.layoutManager = LinearLayoutManager(this)
        rvBillItems.adapter = itemAdapter
    }

    private fun setupListeners() {
        tvBillDate.setOnClickListener { showDatePicker() }

        spinnerArea.setOnItemClickListener { _, _, _, _ ->
            filterCustomers()
        }

        spinnerCustomer.setOnItemClickListener { _, _, position, _ ->
            selectedCustomer = filteredCustomers[position]
        }

        etSearchItems.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterItems(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        spinnerItem.setOnItemClickListener { _, _, position, _ ->
            selectedItem = filteredItems[position]
            etAvailableQty.setText(selectedItem?.stock_quantity?.toString() ?: "0")
            
            // Pre-fill if item already in bill
            val existing = itemAdapter.getItems().find { it.itemId == selectedItem?.id }
            if (existing != null) {
                etQty.setText(existing.quantity.toString())
                etDiscount.setText(existing.discount.toString())
                btnAddItem.text = "Update Item"
            } else {
                etQty.setText("1")
                etDiscount.setText("0")
                btnAddItem.text = "Add Item to Bill"
            }
        }

        btnAddItem.setOnClickListener { 
            if (selectedItem != null) {
                val qty = etQty.text.toString().toIntOrNull() ?: 1
                val disc = etDiscount.text.toString().toDoubleOrNull() ?: 0.0
                
                val totalDisc = disc * qty
                
                itemAdapter.addItem(BillItemForCreation(
                    itemId = selectedItem!!.id, 
                    quantity = qty, 
                    discount = disc,
                    totalDiscount = totalDisc,
                    itemName = selectedItem!!.name,
                    price = selectedItem!!.selling_price?.replace("₹", "")?.replace(",", "")?.toDoubleOrNull()
                ))
                updateSummary()
                
                // Clear inputs
                resetItemInputs()
            } else {
                Toast.makeText(this, "Please select an item", Toast.LENGTH_SHORT).show()
            }
        }

        btnGenerateBill.setOnClickListener { saveBill() }
    }

    private fun resetItemInputs() {
        etQty.setText("1")
        etDiscount.setText("0")
        spinnerItem.setText("", false)
        selectedItem = null
        etAvailableQty.setText("0")
        btnAddItem.text = "Add Item to Bill"
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
        tvBillDate.setText(sdf.format(calendar.time))
    }

    private fun fetchInitialData() {
        lifecycleScope.launch {
            showScreenLoading()
            try {
                val customersDeferred = async { ApiClient.billsDashboardApi.getCustomersForBill() }
                val itemsDeferred = async { fetchAllAvailableItems() }
                
                allCustomers = customersDeferred.await().customers ?: emptyList()
                allAvailableItems = itemsDeferred.await()

                val areas = (listOf("All Areas") + allCustomers.mapNotNull { it.area }.distinct().filter { it.isNotEmpty() }).sorted()
                val areaAdapter = ArrayAdapter(this@CreateBillActivity, android.R.layout.simple_dropdown_item_1line, areas)
                spinnerArea.setAdapter(areaAdapter)
                
                spinnerArea.setText("All Areas", false)
                filterCustomers()
                filterItems("")
                itemAdapter.updateAvailableItems(allAvailableItems)

                billId?.let { id ->
                    val billDetail = ApiClient.billsDashboardApi.getBillDetail(id)
                    val billItems = ApiClient.billsDashboardApi.getBillItems(id)
                    
                    val customer = allCustomers.find { it.name == billDetail.customer }
                    customer?.let {
                        selectedCustomer = it
                        spinnerArea.setText(it.area ?: "All Areas", false)
                        filterCustomers()
                        spinnerCustomer.setText(it.name ?: "", false)
                        selectedCustomer = it
                    }

                    // Date parsing
                    try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        billDetail.invoice_date?.let {
                            calendar.time = sdf.parse(it) ?: calendar.time
                            updateDateDisplay()
                        }
                    } catch (e: Exception) {}

                    // MATCHING LOGIC FOR MISSING IDs
                    val itemsForCreation = billItems.map { detail ->
                        // If detail.item_id is 0 or null, we find it by name from allAvailableItems
                        val resolvedId = if (detail.item_id != 0) detail.item_id 
                                        else allAvailableItems.find { it.name == detail.item_name }?.id ?: 0

                        BillItemForCreation(
                            itemId = resolvedId,
                            quantity = detail.quantity,
                            discount = detail.discount,
                            totalDiscount = detail.total_discount,
                            itemName = detail.item_name,
                            price = detail.price_per_unit
                        )
                    }
                    
                    itemAdapter.updateItems(itemsForCreation)
                    updateSummary()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@CreateBillActivity,
                    NetworkMessageUtils.friendlyMessage(e, "Failed to load bill data"),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                hideScreenLoading()
            }
        }
    }

    private fun filterCustomers() {
        val selectedArea = spinnerArea.text.toString()
        filteredCustomers = if (selectedArea == "All Areas" || selectedArea.isEmpty()) {
            allCustomers
        } else {
            allCustomers.filter { it.area == selectedArea }
        }

        val names = filteredCustomers.map { it.name ?: "" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, names)
        spinnerCustomer.setAdapter(adapter)
        
        spinnerCustomer.setText("", false)
        selectedCustomer = null
    }

    private fun filterItems(query: String) {
        filteredItems = if (query.isEmpty()) {
            allAvailableItems
        } else {
            allAvailableItems.filter { it.name.contains(query, ignoreCase = true) }
        }

        val names = filteredItems.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, names)
        spinnerItem.setAdapter(adapter)
    }

    private fun updateSummary() {
        val items = itemAdapter.getItems()
        var totalQty = 0
        var totalDiscount = 0.0
        var grandTotal = 0.0

        for (billItem in items) {
            val item = allAvailableItems.find { it.id == billItem.itemId }
            val priceStr = item?.selling_price?.replace("₹", "")?.replace(",", "")
            val price = priceStr?.toDoubleOrNull() ?: billItem.price ?: 0.0
            
            totalQty += billItem.quantity
            totalDiscount += billItem.totalDiscount
            grandTotal += (price * billItem.quantity) - billItem.totalDiscount
        }

        tvTotalQty.text = totalQty.toString()
        tvTotalDiscAmount.text = "₹%.2f".format(totalDiscount)
        val roundedGrandTotal = ceil(grandTotal)
        tvGrandTotal.text = "₹%.0f".format(roundedGrandTotal)
    }

    private suspend fun fetchAllAvailableItems(): List<AdminItem> {
        val response = ApiClient.adminItemsApi.getCategories()
        return response.categories.map { category ->
            lifecycleScope.async { ApiClient.adminItemsApi.getItemsByCategory(category) }
        }.awaitAll().flatMap { it.items }.distinctBy { it.id }
    }

    private fun saveBill() {
        btnGenerateBill.isEnabled = false
        showScreenLoading()
        lifecycleScope.launch {
            try {
                if (billId == null) createBill() else editBill(billId!!)
            } catch (e: Exception) {
                btnGenerateBill.isEnabled = true
                hideScreenLoading()
                Toast.makeText(
                    this@CreateBillActivity,
                    NetworkMessageUtils.friendlyMessage(e, "Failed to save bill"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun createBill() {
        if (selectedCustomer == null) {
            btnGenerateBill.isEnabled = true
            hideScreenLoading()
            Toast.makeText(this, "Please select a customer", Toast.LENGTH_SHORT).show()
            return
        }
        val customerId = selectedCustomer?.id ?: 0

        val billItems = itemAdapter.getItems().filter { it.itemId != 0 }
        if (billItems.isEmpty()) {
            btnGenerateBill.isEnabled = true
            hideScreenLoading()
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
        btnGenerateBill.isEnabled = true
        hideScreenLoading()
        Toast.makeText(this, "Bill created successfully!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private suspend fun editBill(id: Int) {
        val billItems = itemAdapter.getItems().filter { it.itemId != 0 }
        if (billItems.isEmpty()) {
            btnGenerateBill.isEnabled = true
            hideScreenLoading()
            Toast.makeText(this, "Please add at least one item", Toast.LENGTH_SHORT).show()
            return
        }

        val request = EditBillRequest(
            items = billItems.map { it.itemId },
            quantities = billItems.map { it.quantity },
            discounts = billItems.map { it.discount },
            customerId = selectedCustomer?.id,
            invoiceDate = tvBillDate.text.toString()
        )
        ApiClient.billsDashboardApi.editBill(id, request)
        btnGenerateBill.isEnabled = true
        hideScreenLoading()
        Toast.makeText(this, "Bill updated successfully!", Toast.LENGTH_SHORT).show()
        finish()
    }
}
