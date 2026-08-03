package com.svd.svdagencies.ui.delivery

import com.svd.svdagencies.utils.PaymentConfig

import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
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
import com.svd.svdagencies.data.model.delivery.*
import com.svd.svdagencies.utils.SessionManager
import kotlinx.coroutines.launch
import retrofit2.awaitResponse
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DeliveryBillToCustomerActivity : BaseActivity() {

    private var customerId: Int = 0
    private var customerName: String = ""
    private var openingDue: Double = 0.0

    private lateinit var rvItemCatalog: RecyclerView
    private lateinit var btnGenerateBill: MaterialButton
    private lateinit var btnClearSelection: MaterialButton
    private lateinit var toggleBillMode: MaterialButtonToggleGroup
    private lateinit var btnShowQr: ImageButton
    
    private lateinit var tvItemsTotal: TextView
    private lateinit var tvOpeningDue: TextView
    private lateinit var tvGrandTotal: TextView
    private lateinit var etCollectedAmount: TextInputEditText
    
    private lateinit var btnViewHistory: ImageButton
    private lateinit var autoCustomer: android.widget.AutoCompleteTextView
    private lateinit var autoRoute: android.widget.AutoCompleteTextView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var swipeRefresh: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var toolbar: com.google.android.material.appbar.MaterialToolbar

    private lateinit var catalogAdapter: DeliveryBillSelectAdapter
    private lateinit var sessionManager: SessionManager
    private var availableItems: List<DeliveryBillItem> = emptyList()
    private var customers: List<DeliveryBillCustomer> = emptyList()
    private var routes: List<DeliveryRoute> = emptyList()
    private var selectedRouteId: Int? = null
    private var currentItemsTotal: Double = 0.0
    private var currentGrandTotal: Double = 0.0
    private var billMode: String = BILL_MODE_REGULAR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.delivery_bill_to_customer)
        sessionManager = SessionManager(this)

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
        btnGenerateBill.setOnClickListener { generateBill() }
        toggleBillMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            billMode = if (checkedId == R.id.btnModeSpecial) BILL_MODE_SPECIAL else BILL_MODE_REGULAR
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

                    val savedRouteId = sessionManager.getSelectedRouteId()
                    val savedRouteName = sessionManager.getSelectedRouteName()

                    if (savedRouteId != null && routes.any { it.id == savedRouteId }) {
                        selectedRouteId = savedRouteId
                        autoRoute.setText(savedRouteName, false)
                    } else {
                        selectedRouteId = null
                        autoRoute.setText(savedRouteName ?: "All Routes", false)
                    }

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
                    
                    // fetchCustomers() was here - removed to prevent automatic fetch
                    hideScreenLoading()
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
        lifecycleScope.launch {
            if (!swipeRefresh.isRefreshing && customers.isEmpty()) showScreenLoading()
            try {
                val response = ApiClient.deliveryApi.getBillCustomers(routeId = selectedRouteId).awaitResponse()
                if (response.isSuccessful) {
                    customers = response.body()?.results ?: emptyList()
                    val labels = customers.map { it.label }
                    autoCustomer.setAdapter(
                        ArrayAdapter(this@DeliveryBillToCustomerActivity, android.R.layout.simple_dropdown_item_1line, labels)
                    )
                    autoCustomer.setOnItemClickListener { _, _, position, _ ->
                        customers.getOrNull(position)?.let { customer ->
                            customerId = customer.id
                            customerName = customer.name
                            fetchOpeningBalance(customer.id)
                            
                            catalogAdapter.submitList(emptyList())
                            fetchItems()
                        }
                    }
                } else {
                    Toast.makeText(this@DeliveryBillToCustomerActivity, "Failed to load customers", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@DeliveryBillToCustomerActivity, "Failed to load customers", Toast.LENGTH_SHORT).show()
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
                    val rawItems = responseBody?.items ?: emptyList()
                    val orderMap = buildItemOrderMap(responseBody, rawItems)
                    availableItems = rawItems.sortedWith(compareBy<DeliveryBillItem> {
                        val cleanCode = it.code.trim().lowercase()
                        orderMap[cleanCode] ?: Int.MAX_VALUE
                    }.thenBy { it.name.lowercase(Locale.ROOT) })
                    catalogAdapter.submitList(availableItems)
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
        
        currentGrandTotal = currentItemsTotal + openingDue
        val roundedGrandTotal = kotlin.math.ceil(currentGrandTotal)
        tvGrandTotal.text = "₹ %.0f".format(roundedGrandTotal)
        currentGrandTotal = roundedGrandTotal
        
        val canGenerate = selectedItems.isNotEmpty() && customerId > 0
        btnGenerateBill.isEnabled = canGenerate
        btnGenerateBill.alpha = if (canGenerate) 1.0f else 0.5f
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

    private fun showConfirmationDialog(selectedItems: List<Pair<DeliveryBillItem, Int>>) {
        val dialogView = layoutInflater.inflate(R.layout.delivery_bill_confirmation, null)
        val rvConfirmItems = dialogView.findViewById<RecyclerView>(R.id.rvConfirmItems)
        val tvConfirmCustomer = dialogView.findViewById<TextView>(R.id.tvConfirmCustomer)
        val tvConfirmTotal = dialogView.findViewById<TextView>(R.id.tvConfirmTotal)
        val btnUpi = dialogView.findViewById<MaterialButton>(R.id.btnUpi)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btnConfirm)

        tvConfirmCustomer.text = "Customer: $customerName"
        tvConfirmTotal.text = "₹ %.2f".format(currentGrandTotal)

        rvConfirmItems.adapter = DeliveryBillConfirmationAdapter(selectedItems)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            processBillGeneration(selectedItems, showQr = false)
        }

        btnUpi.setOnClickListener {
            dialog.dismiss()
            processBillGeneration(selectedItems, showQr = true)
        }

        dialog.show()
    }

    private fun processBillGeneration(selectedItems: List<Pair<DeliveryBillItem, Int>>, showQr: Boolean) {
        lifecycleScope.launch {
            showScreenLoading()
            try {
                val collectedAmountValue = etCollectedAmount.text.toString().toDoubleOrNull() ?: 0.0
                
                val request = DeliveryGenerateBillRequest(
                    customerId = customerId,
                    billDate = getCurrentBillDate(),
                    items = selectedItems.map { BillLineItem(it.first.itemId, it.second, discountForItem(it.first, it.second)) },
                    paidAmount = collectedAmountValue,
                    billMode = billMode
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
                hideScreenLoading()
            }
        }
    }

    private fun resetLayout() {
        catalogAdapter.resetQuantities()
        etCollectedAmount.setText("")
        autoCustomer.setText("")
        customerId = 0
        customerName = ""
        openingDue = 0.0
        tvOpeningDue.text = "₹ 0.00"
        updateSummary()
        fetchCustomers() // Refresh customer list for the same route
    }

    private fun getCurrentBillDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Calendar.getInstance().time)
    }

    private fun discountForItem(item: DeliveryBillItem, quantity: Int): Double {
        val code = item.code.trim().lowercase(Locale.ROOT)
        return when {
            billMode != BILL_MODE_SPECIAL -> 0.0
            code == ITEM_CODE_FCM500 -> FCM500_SPECIAL_DISCOUNT
            code == ITEM_CODE_CURD450 -> CURD450_SPECIAL_DISCOUNT
            code == ITEM_CODE_CURD120 && quantity >= CURD120_SPECIAL_MIN_QTY -> CURD120_SPECIAL_DISCOUNT
            else -> 0.0
        }
    }

    private fun discountedLineTotal(item: DeliveryBillItem, quantity: Int): Double {
        val discountedPrice = (item.price - discountForItem(item, quantity)).coerceAtLeast(0.0)
        return discountedPrice * quantity
    }

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
        private const val FCM500_SPECIAL_DISCOUNT = 0.5
        private const val CURD450_SPECIAL_DISCOUNT = 0.3
        private const val CURD120_SPECIAL_DISCOUNT = 0.25
        private const val CURD120_SPECIAL_MIN_QTY = 96
    }
}
