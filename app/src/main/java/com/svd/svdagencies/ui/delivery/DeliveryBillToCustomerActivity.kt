package com.svd.svdagencies.ui.delivery

import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.svd.svdagencies.utils.PaymentConfig

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.svd.svdagencies.R
import com.svd.svdagencies.base.BaseActivity
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.customerData.UpdateBalanceRequest
import com.svd.svdagencies.data.model.delivery.*
import com.svd.svdagencies.utils.NetworkMessageUtils
import com.svd.svdagencies.utils.SessionManager
import com.svd.svdagencies.utils.showLoading
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class DeliveryBillToCustomerActivity : BaseActivity() {

    private var customerId: Int = 0
    private var customerName: String = ""
    private var customerPhone: String = ""
    private var customerUserType: String = "user"
    private var openingDue: Double = 0.0

    private lateinit var rvItemCatalog: RecyclerView
    private lateinit var btnGenerateBill: MaterialButton
    private lateinit var btnClearSelection: MaterialButton
    private lateinit var toggleBillMode: MaterialButtonToggleGroup
    private lateinit var btnShowQr: ImageButton
    
    private lateinit var tvItemsTotal: TextView
    private lateinit var tvOpeningDue: TextView
    private lateinit var tvGrandTotal: TextView
    private lateinit var etCollectedAmount: EditText
    
    private lateinit var btnViewHistory: ImageButton
    private lateinit var autoCustomer: android.widget.AutoCompleteTextView
    private lateinit var autoRoute: android.widget.AutoCompleteTextView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var swipeRefresh: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var toolbar: com.google.android.material.appbar.MaterialToolbar

    private lateinit var catalogAdapter: DeliveryBillSelectAdapter
    private lateinit var sessionManager: SessionManager
    private var fetchCustomersJob: Job? = null
    private var availableItems: List<DeliveryBillItem> = emptyList()
    private var customers: List<DeliveryBillCustomer> = emptyList()
    private var routes: List<DeliveryRoute> = emptyList()
    private var selectedRouteId: Int? = null
    private var currentItemsTotal: Double = 0.0
    private var currentGrandTotal: Double = 0.0
    private var pendingTransactionId: String? = null
    private var billMode: String = BILL_MODE_REGULAR
    private val selectedDiscountItemCodes = mutableSetOf<String>()
    private var prefillMap: Map<Int, Int>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.delivery_bill_to_customer)
        sessionManager = SessionManager(this)

        if (savedInstanceState != null) {
            val ids = savedInstanceState.getIntArray("saved_prefill_ids")
            val qtys = savedInstanceState.getIntArray("saved_prefill_qtys")
            if (ids != null && qtys != null && ids.size == qtys.size) {
                prefillMap = ids.zip(qtys).toMap()
            }
        }

        initViews()
        setSupportActionBar(toolbar)
        DeliveryNavigation.setup(
            this,
            drawerLayout,
            navigationView,
            toolbar = toolbar,
            selectedItemId = R.id.nav_delivery_bill_customer
        )

        setupRecyclerViews()
        setupListeners()
        fetchRoutes()
        updateSummary()

        if (intent.getBooleanExtra("open_customer_picker", false)) {
            autoCustomer.post {
                if (!isFinishing) {
                    autoCustomer.showDropDown()
                }
            }
        }
    }

    private fun initViews() {
        rvItemCatalog = findViewById(R.id.rvItemCatalog)
        btnGenerateBill = findViewById(R.id.btnGenerateBill)
        btnClearSelection = findViewById(R.id.btnClearSelection)
        toggleBillMode = findViewById(R.id.toggleBillMode)
        tvItemsTotal = findViewById(R.id.tvItemsTotal)
        tvOpeningDue = findViewById(R.id.tvOpeningDue)
        tvGrandTotal = findViewById(R.id.tvGrandTotal)
        etCollectedAmount = findViewById(R.id.etCollectedAmount)
        btnViewHistory = findViewById(R.id.btnViewHistory)
        btnShowQr = findViewById(R.id.btnShowQr)
        autoCustomer = findViewById(R.id.autoCustomer)
        autoRoute = findViewById(R.id.autoRoute)
        drawerLayout = findViewById(R.id.deliveryDrawerLayout)
        navigationView = findViewById(R.id.deliveryNavigationView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        toolbar = findViewById(R.id.toolbar)
    }

    private fun setupRecyclerViews() {
        catalogAdapter = DeliveryBillSelectAdapter { _, _ ->
            updateSummary()
        }
        rvItemCatalog.layoutManager = GridLayoutManager(this, 2)
        rvItemCatalog.adapter = catalogAdapter
    }

    private fun setupListeners() {
        btnGenerateBill.setOnClickListener {
            val selectedItems = catalogAdapter.getSelectedItemsWithQty()
            if (selectedItems.isNotEmpty()) {
                generateBill()
            } else {
                showUpdateBalanceDialog()
            }
        }
        toggleBillMode.isSingleSelection = false
        toggleBillMode.isSelectionRequired = false
        toggleBillMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (checkedId == R.id.btnModeSpecial) {
                billMode = if (isChecked) "special" else "regular"
            } else {
                val itemCode = when (checkedId) {
                    R.id.btnModeCurd120 -> ITEM_CODE_CURD120
                    R.id.btnModeCurd450 -> ITEM_CODE_CURD450
                    else -> ITEM_CODE_FCM500
                }
                if (isChecked) {
                    selectedDiscountItemCodes.add(itemCode)
                } else {
                    selectedDiscountItemCodes.remove(itemCode)
                }
            }
            catalogAdapter.submitListPreservingQuantities(availableItems)
            updateSummary()
        }
        btnClearSelection.setOnClickListener {
            catalogAdapter.resetQuantities()
            etCollectedAmount.setText("")
            updateSummary()
        }
        btnShowQr.setOnClickListener { showPaymentQr() }
        btnViewHistory.setOnClickListener { 
            if (customerId > 0) {
                val intent = android.content.Intent(this, DeliveryBillHistoryActivity::class.java).apply {
                    putExtra("customer_id", customerId)
                    putExtra("customer_name", customerName)
                    putExtra("customer_phone", customerPhone)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Select a customer first", Toast.LENGTH_SHORT).show()
            }
        }
        btnViewHistory.setOnLongClickListener {
            if (customerId > 0) {
                val intent = android.content.Intent(this, DeliveryCustomerPaymentsActivity::class.java).apply {
                    putExtra("customer_id", customerId)
                    putExtra("customer_name", customerName)
                }
                startActivity(intent)
                true
            } else {
                false
            }
        }
        swipeRefresh.setOnRefreshListener { 
            if (customerId > 0) {
                fetchItems()
            } else {
                fetchRoutes()
            }
        }
    }

    private fun fetchRoutes() {
        lifecycleScope.launch {
            if (!swipeRefresh.isRefreshing) showScreenLoading()
            try {
                val response = ApiClient.deliveryApi.getRoutes().awaitResponse()
                if (response.isSuccessful) {
                    routes = response.body().orEmpty()
                    val displayRoutes = mutableListOf("All Routes")
                    displayRoutes.addAll(routes.map { it.name })
                    
                    autoRoute.setAdapter(
                        ArrayAdapter(this@DeliveryBillToCustomerActivity, android.R.layout.simple_dropdown_item_1line, displayRoutes)
                    )

                    autoRoute.setOnItemClickListener { _, _, position, _ ->
                        val newRouteId = if (position == 0) null else routes.getOrNull(position - 1)?.id
                        val newRouteName = if (position == 0) "All Routes" else routes.getOrNull(position - 1)?.name
                        
                        if (selectedRouteId != newRouteId) {
                            selectedRouteId = newRouteId
                            sessionManager.saveSelectedRoute(selectedRouteId, newRouteName)
                            autoCustomer.setText("")
                            customerId = 0
                            fetchCustomers()
                        }
                    }
                    
                    fetchCustomers()
                } else {
                    swipeRefresh.isRefreshing = false
                    hideScreenLoading()
                }
            } catch (e: Exception) {
                hideScreenLoading()
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun fetchCustomers() {
        fetchCustomersJob?.cancel()
        
        // Clear previous data immediately to prevent wrong selection
        customers = emptyList()
        autoCustomer.setAdapter(null as ArrayAdapter<String>?)
        
        fetchCustomersJob = lifecycleScope.launch {
            if (!swipeRefresh.isRefreshing) {
                showScreenLoading()
            }
            
            try {
                val response = ApiClient.deliveryApi.getBillCustomers(routeId = selectedRouteId).awaitResponse()
                if (response.isSuccessful) {
                    customers = response.body()?.results ?: emptyList()
                    val labels = withContext(Dispatchers.Default) {
                        customers.map { it.label }
                    }
                    
                    autoCustomer.setAdapter(
                        ArrayAdapter(this@DeliveryBillToCustomerActivity, android.R.layout.simple_dropdown_item_1line, labels)
                    )
                    
                    autoCustomer.setOnItemClickListener { _, _, position, _ ->
                        customers.getOrNull(position)?.let { customer ->
                            customerId = customer.id
                            customerName = customer.name
                            customerPhone = customer.phone.orEmpty()
                            customerUserType = customer.userType ?: "user"
                            fetchOpeningBalance(customer.id)
                            
                            catalogAdapter.setUserType(customerUserType)
                            catalogAdapter.submitList(emptyList())
                            fetchItems()
                        }
                    }
                    
                    // Automatically show dropdown if the user just selected a route
                    if (customers.isNotEmpty() && !swipeRefresh.isRefreshing) {
                        autoCustomer.showDropDown()
                    }
                } else {
                    Toast.makeText(this@DeliveryBillToCustomerActivity, "Failed to load customers", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Toast.makeText(this@DeliveryBillToCustomerActivity, "Failed to load customers", Toast.LENGTH_SHORT).show()
                }
            } finally {
                hideScreenLoading()
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun fetchOpeningBalance(id: Int) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.deliveryApi.getCustomerOpeningBalance(id).awaitResponse()
                if (response.isSuccessful) {
                    val body = response.body()
                    openingDue = body?.openingBalance ?: 0.0
                    tvOpeningDue.text = "₹ %.2f".format(openingDue)
                    updateSummary()
                }
            } catch (e: Exception) {
                openingDue = 0.0
                tvOpeningDue.text = "₹ 0.00"
                updateSummary()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::catalogAdapter.isInitialized) {
            val selected = catalogAdapter.getSelectedItemsWithQty()
            if (selected.isNotEmpty()) {
                val ids = selected.map { it.first.itemId }.toIntArray()
                val qtys = selected.map { it.second }.toIntArray()
                outState.putIntArray("saved_prefill_ids", ids)
                outState.putIntArray("saved_prefill_qtys", qtys)
            }
        }
    }

    private fun fetchItems() {
        if (customerId <= 0) {
            swipeRefresh.isRefreshing = false
            return
        }

        lifecycleScope.launch {
            if (!swipeRefresh.isRefreshing && availableItems.isEmpty()) showScreenLoading()
            try {
                val catalogResponse = ApiClient.deliveryApi.getBillItems(customerId).awaitResponse()
                if (catalogResponse.isSuccessful) {
                    val responseBody = catalogResponse.body()
                    val rawItems = responseBody?.items?.filter { it.stockQuantity > 0 } ?: emptyList()
                    val orderMap = buildItemOrderMap(responseBody, rawItems)
                    availableItems = rawItems.sortedWith(compareBy<DeliveryBillItem> {
                        val cleanCode = it.code.trim().lowercase()
                        orderMap[cleanCode] ?: Int.MAX_VALUE
                    }.thenBy { it.name.lowercase(Locale.ROOT) })
                    
                    catalogAdapter.submitListPreservingQuantities(availableItems)
                    
                    prefillMap?.let {
                        catalogAdapter.setInitialQuantities(it)
                        prefillMap = null
                    }
                    updateSummary()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DeliveryBillToCustomerActivity, "Failed to load data", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh.isRefreshing = false
                hideScreenLoading()
            }
        }
    }

    private fun updateSummary() {
        val selectedItems = catalogAdapter.getSelectedItemsWithQty()
        currentItemsTotal = 0.0
        for (pair in selectedItems) {
            val item = pair.first
            val qty = pair.second
            currentItemsTotal += discountedLineTotal(item, qty)
        }
        
        tvItemsTotal.text = "₹ %.2f".format(currentItemsTotal)
        tvOpeningDue.text = "₹ %.2f".format(openingDue)
        
        currentGrandTotal = currentItemsTotal + openingDue
        val roundedGrandTotal = kotlin.math.ceil(currentGrandTotal)
        tvGrandTotal.text = "₹ %.0f".format(roundedGrandTotal)
        currentGrandTotal = roundedGrandTotal
        
        if (selectedItems.isNotEmpty()) {
            btnGenerateBill.text = "Generate"
            btnGenerateBill.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.green_status)
            )
            val canGenerate = customerId > 0
            btnGenerateBill.isEnabled = canGenerate
            btnGenerateBill.alpha = if (canGenerate) 1.0f else 0.5f
        } else {
            btnGenerateBill.text = "Update Due"
            btnGenerateBill.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.brand_blue)
            )
            val canUpdate = customerId > 0
            btnGenerateBill.isEnabled = canUpdate
            btnGenerateBill.alpha = if (canUpdate) 1.0f else 0.5f
        }
    }

    private fun showUpdateBalanceDialog() {
        if (customerId <= 0) {
            Toast.makeText(this, "Please select a customer first", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.admin_customer_balance_update, null)
        val txtCustomerName = dialogView.findViewById<TextView>(R.id.txtCustomerName)
        val txtCurrentBalance = dialogView.findViewById<TextView>(R.id.txtCurrentBalance)
        val etAmount = dialogView.findViewById<TextInputEditText>(R.id.etAmount)
        val btnClose = dialogView.findViewById<View>(R.id.btnClose)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnUpdate = dialogView.findViewById<MaterialButton>(R.id.btnUpdate)
        val chip500 = dialogView.findViewById<View>(R.id.chipAdd500)
        val chip1000 = dialogView.findViewById<View>(R.id.chipAdd1000)
        val chipClear = dialogView.findViewById<View>(R.id.chipClear)

        txtCustomerName?.text = customerName
        txtCurrentBalance?.text = "₹ %.2f".format(openingDue)

        val collectedVal = etCollectedAmount.text.toString().trim()
        if (collectedVal.isNotEmpty()) {
            etAmount?.setText(collectedVal)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnClose?.setOnClickListener { dialog.dismiss() }
        btnCancel?.setOnClickListener { dialog.dismiss() }

        chip500?.setOnClickListener {
            val current = etAmount?.text?.toString()?.toDoubleOrNull() ?: 0.0
            etAmount?.setText("%.2f".format(current + 500.0))
        }

        chip1000?.setOnClickListener {
            val current = etAmount?.text?.toString()?.toDoubleOrNull() ?: 0.0
            etAmount?.setText("%.2f".format(current + 1000.0))
        }

        chipClear?.setOnClickListener {
            etAmount?.setText("")
        }

        btnUpdate?.setOnClickListener {
            val amountStr = etAmount?.text?.toString()?.trim().orEmpty()
            if (amountStr.isEmpty()) {
                etAmount?.error = "Please enter amount"
                return@setOnClickListener
            }

            btnUpdate.showLoading(true, "Updating...")
            btnCancel?.isEnabled = false

            lifecycleScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        ApiClient.deliveryApi.saveAgentCollection(customerId, amountStr)
                    }
                    dialog.dismiss()
                    if (response.success) {
                        Toast.makeText(this@DeliveryBillToCustomerActivity, response.message ?: "Balance updated", Toast.LENGTH_SHORT).show()
                        etCollectedAmount.setText("")
                        fetchOpeningBalance(customerId)
                    } else {
                        Toast.makeText(this@DeliveryBillToCustomerActivity, response.message ?: "Failed to update balance", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    dialog.dismiss()
                    Toast.makeText(
                        this@DeliveryBillToCustomerActivity,
                        NetworkMessageUtils.friendlyMessage(e, "Failed to update balance"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        dialog.show()
    }

    private fun showPaymentQr(
        amount: Double? = null,
        billNumber: String? = null,
        customerId: Int? = null,
        customerName: String? = null
    ) {
        val qrAmount = amount ?: currentGrandTotal
        if (qrAmount <= 0) {
            Toast.makeText(this, "Select items first", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_qr_code, null)
        val ivQrCode = dialogView.findViewById<android.widget.ImageView>(R.id.ivQrCode)
        val tvQrAmount = dialogView.findViewById<TextView>(R.id.tvQrAmount)
        val tvBillInfo = dialogView.findViewById<TextView>(R.id.tvBillInfo)
        val btnCloseQr = dialogView.findViewById<MaterialButton>(R.id.btnCloseQr)

        tvQrAmount.text = "Amount: ₹%.2f".format(qrAmount)
        
        if (billNumber != null && customerId != null) {
            tvBillInfo.text = "Bill: $billNumber | ID: $customerId"
            tvBillInfo.visibility = android.view.View.VISIBLE
        }

        val upiId = PaymentConfig.UPI_ID
        val name = "Sri Vijay Durga Milk Agency"
        
        val paymentMsg = if (billNumber != null && customerName != null) {
            "($billNumber-$customerName)"
        } else {
            ""
        }
        
        val upiUrl = "upi://pay?pa=$upiId&pn=${Uri.encode(name)}&am=${"%.2f".format(qrAmount)}&cu=INR&tn=${Uri.encode(paymentMsg)}"

        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(upiUrl, BarcodeFormat.QR_CODE, 512, 512)
            ivQrCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        btnCloseQr.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun generateBill() {
        if (customerId <= 0) {
            Toast.makeText(this, "Please select a customer", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedItems = catalogAdapter.getSelectedItemsWithQty()
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "Add items to generate bill", Toast.LENGTH_SHORT).show()
            return
        }

        showConfirmationDialog(selectedItems)
    }

    /** Previous due is displayed separately; overpayments remain customer credit. */
    private fun validateCollectedAmount(requirePayment: Boolean): Double? {
        val collected = etCollectedAmount.text.toString().toDoubleOrNull() ?: 0.0
        if (collected < 0.0) {
            etCollectedAmount.error = "Collected amount cannot be negative"
            return null
        }
        if (requirePayment && collected <= 0.0) {
            etCollectedAmount.error = "Enter the amount collected before showing the UPI QR"
            return null
        }
        return collected
    }

    private fun showConfirmationDialog(selectedItems: List<Pair<DeliveryBillItem, Int>>) {
        val dialogView = layoutInflater.inflate(R.layout.delivery_bill_confirmation, null)
        val rvConfirmItems = dialogView.findViewById<RecyclerView>(R.id.rvConfirmItems)
        val tvConfirmCustomer = dialogView.findViewById<TextView>(R.id.tvConfirmCustomer)
        val tvConfirmTotal = dialogView.findViewById<TextView>(R.id.tvConfirmTotal)
        val tvConfirmCollected = dialogView.findViewById<TextView>(R.id.tvConfirmCollected)
        val tvConfirmRemainingDue = dialogView.findViewById<TextView>(R.id.tvConfirmRemainingDue)
        val layoutCollected = dialogView.findViewById<View>(R.id.layoutCollectedAmount)
        val layoutRemaining = dialogView.findViewById<View>(R.id.layoutRemainingDue)
        
        val btnUpi = dialogView.findViewById<MaterialButton>(R.id.btnUpi)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btnConfirm)

        tvConfirmCustomer.text = "Customer: $customerName"
        tvConfirmTotal.text = "₹ %.2f".format(currentGrandTotal)

        val collected = etCollectedAmount.text.toString().toDoubleOrNull() ?: 0.0
        if (collected > 0) {
            layoutCollected.visibility = View.VISIBLE
            layoutRemaining.visibility = View.VISIBLE
            tvConfirmCollected.text = "₹ %.2f".format(collected)
            tvConfirmRemainingDue.text = "₹ %.2f".format(currentGrandTotal - collected)
        } else {
            layoutCollected.visibility = View.GONE
            layoutRemaining.visibility = View.GONE
        }

        rvConfirmItems.adapter = DeliveryBillConfirmationAdapter(selectedItems, ::customerUnitPrice, ::discountForItem)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        btnConfirm.setOnClickListener {
            processBillGeneration(selectedItems, showQr = false, btnConfirm, btnUpi, dialog)
        }

        btnUpi.setOnClickListener {
            processBillGeneration(selectedItems, showQr = true, btnUpi, btnConfirm, dialog)
        }

        dialog.show()
    }

    private fun processBillGeneration(
        selectedItems: List<Pair<DeliveryBillItem, Int>>, 
        showQr: Boolean,
        btn: MaterialButton,
        otherBtn: MaterialButton,
        dialog: DialogInterface
    ) {
        val collectedAmountValue = validateCollectedAmount(showQr) ?: run {
            otherBtn.isEnabled = true
            return
        }
        btn.showLoading(true, "Generating...")
        otherBtn.isEnabled = false
        
        lifecycleScope.launch {
            try {
                val transactionId = pendingTransactionId ?: UUID.randomUUID().toString().also {
                    pendingTransactionId = it
                }
                val request = DeliveryGenerateBillRequest(
                    customerId = customerId,
                    billDate = getCurrentBillDate(),
                    items = selectedItems.map { BillLineItem(it.first.itemId, it.second, discountForItem(it.first, it.second)) },
                    paidAmount = collectedAmountValue,
                    paymentMethod = if (showQr) "UPI" else if (collectedAmountValue > 0) "CASH" else null,
                    transactionId = transactionId,
                    billMode = billMode,
                    discountItemCodes = selectedDiscountItemCodes.toList()
                )
                
                val response = ApiClient.deliveryApi.generateBill(request).awaitResponse()
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@DeliveryBillToCustomerActivity, "Bill Generated", Toast.LENGTH_SHORT).show()
                    
                    val responseBody = response.body()
                    val amountToPay = currentGrandTotal
                    
                    if (showQr) {
                        showPaymentQr(
                            amount = amountToPay,
                            billNumber = responseBody?.bill?.invoiceNumber,
                            customerId = responseBody?.bill?.customerId,
                            customerName = responseBody?.bill?.customerName
                        )
                    }
                    
                    catalogAdapter.applyStockDeductions(selectedItems)
                    pendingTransactionId = null
                    dialog.dismiss()
                    resetLayout()
                } else {
                    Toast.makeText(
                        this@DeliveryBillToCustomerActivity,
                        com.svd.svdagencies.utils.NetworkMessageUtils.parseError(response, response.body()?.message ?: "Failed"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DeliveryBillToCustomerActivity, "Error generating bill", Toast.LENGTH_SHORT).show()
            } finally {
                btn.showLoading(false)
                otherBtn.isEnabled = true
            }
        }
    }

    private fun resetLayout() {
        catalogAdapter.resetQuantities()
        etCollectedAmount.setText("")
        selectedDiscountItemCodes.clear()
        toggleBillMode.clearChecked()
        
        // Refresh items and opening balance for the same customer
        if (customerId > 0) {
            fetchOpeningBalance(customerId)
            fetchItems()
        }
        
        updateSummary()
    }

    private fun getCurrentBillDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Calendar.getInstance().time)
    }

    private fun discountForItem(item: DeliveryBillItem, quantity: Int): Double {
        val code = item.code.trim().lowercase(Locale.ROOT)

        // 1. Check for Special Mode discounts (matching DeliveryCreateBillActivity)
        if (billMode == BILL_MODE_SPECIAL) {
            val specialDiscount = when {
                code == ITEM_CODE_FCM500 -> FCM500_SPECIAL_DISCOUNT
                code == ITEM_CODE_CURD450 -> CURD450_SPECIAL_DISCOUNT
                code == ITEM_CODE_CURD120 && quantity >= CURD120_SPECIAL_MIN_QTY -> CURD120_SPECIAL_DISCOUNT
                else -> 0.0
            }
            if (specialDiscount > 0) return specialDiscount
        }

        // 2. Fallback to manual item-code button toggles
        return if (code in selectedDiscountItemCodes && quantity > 0) ITEM_CODE_BUTTON_DISCOUNT else 0.0
    }

    private fun discountedLineTotal(item: DeliveryBillItem, quantity: Int): Double {
        val basePrice = customerUnitPrice(item)
        val discountedPrice = (basePrice - discountForItem(item, quantity)).coerceAtLeast(0.0)
        return discountedPrice * quantity
    }

    private fun customerUnitPrice(item: DeliveryBillItem): Double = item.price

    private fun buildItemOrderMap(
        responseBody: DeliveryBillItemsResponse?,
        rawItems: List<DeliveryBillItem>
    ): Map<String, Int> {
        val orderedCodes = LinkedHashSet<String>()
        responseBody?.allowedItemCodes
            ?.map { it.trim().lowercase(Locale.ROOT) }
            ?.filterTo(orderedCodes) { it.isNotBlank() }
        rawItems
            .map { it.code.trim().lowercase(Locale.ROOT) }
            .filterTo(orderedCodes) { it.isNotBlank() }
        return orderedCodes.mapIndexed { index, code -> code to index }.toMap()
    }

    companion object {
        private const val BILL_MODE_REGULAR = "regular"
        private const val BILL_MODE_SPECIAL = "special"
        private const val ITEM_CODE_FCM500 = "fcm500"
        private const val ITEM_CODE_CURD450 = "curd450"
        private const val ITEM_CODE_CURD120 = "curd120"
        private const val ITEM_CODE_BUTTON_DISCOUNT = 0.5

        private const val FCM500_SPECIAL_DISCOUNT = 0.5
        private const val CURD450_SPECIAL_DISCOUNT = 0.3
        private const val CURD120_SPECIAL_DISCOUNT = 0.25
        private const val CURD120_SPECIAL_MIN_QTY = 96
    }
}
