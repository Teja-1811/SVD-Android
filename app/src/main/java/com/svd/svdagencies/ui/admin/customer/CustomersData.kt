package com.svd.svdagencies.ui.admin.customer

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.admin.CustomerDashboardApi
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.customerData.CustomerDashboardResponse
import com.svd.svdagencies.data.model.admin.customerData.CustomerItem
import com.svd.svdagencies.data.model.admin.customerData.UpdateBalanceRequest
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import com.svd.svdagencies.ui.admin.adapter.CustomerAdapter
import com.svd.svdagencies.utils.SessionManager
import com.svd.svdagencies.utils.showLoading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

open class CustomersData : AdminBaseActivity() {

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var rvCustomers: androidx.recyclerview.widget.RecyclerView
    private lateinit var adapter: CustomerAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var api: CustomerDashboardApi

    private lateinit var etSearch: EditText
    private lateinit var fabAddCustomer: com.google.android.material.button.MaterialButton
    private lateinit var tvEmptyState: TextView
    private var selectedType = "delivery"

    private var allCustomers: List<CustomerItem> = emptyList()
    private val addCustomerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        loadCustomers()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_customer_dashboard)

        // Setup shared admin UI
        setupAdminLayout("Customers")

        sessionManager = SessionManager(this)
        api = ApiClient.adminCustomerDashboard

        initViews()
        if (this is AdminUsersActivity) {
            selectedType = "user"
            findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupCustomerType).check(R.id.chipUser)
        }
        if (this !is AdminUsersActivity) {
            selectedType = intent.getStringExtra("DEFAULT_CUSTOMER_TYPE")?.takeIf { it in setOf("retailer", "delivery", "user") } ?: selectedType
            findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupCustomerType).check(when (selectedType) {
                "retailer" -> R.id.chipRetailer
                "user" -> R.id.chipUser
                else -> R.id.chipDelivery
            })
        }
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadCustomers()
    }

    private fun initViews() {

        swipeRefreshLayout = findViewById(R.id.swipeRefresh)
        rvCustomers = findViewById(R.id.rvCustomers)
        etSearch = findViewById(R.id.etSearch)
        fabAddCustomer = findViewById(R.id.fabAddCustomer)
        tvEmptyState = findViewById(R.id.tvEmptyState) // Add this in XML

        rvCustomers.layoutManager = LinearLayoutManager(this)
        
        adapter = CustomerAdapter(
            items = emptyList(),
            onFreezeClick = { customer ->
                customer.id?.let { toggleFreeze(it) }
            },
            onBalanceClick = { customer ->
                showUpdateBalanceDialog(customer)
            }
        )
        rvCustomers.adapter = adapter
    }

    private fun setupListeners() {

        swipeRefreshLayout.setOnRefreshListener {
            loadCustomers()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterCustomers(s?.toString().orEmpty())
            }
        })

        fabAddCustomer.setOnClickListener {
            val intent = Intent(this, AddCustomerActivity::class.java)
            intent.putExtra("DEFAULT_CUSTOMER_TYPE", selectedType)
            addCustomerLauncher.launch(intent)
        }

        findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupCustomerType)
            .setOnCheckedStateChangeListener { _, checkedIds ->
                selectedType = when (checkedIds.firstOrNull()) {
                    R.id.chipRetailer -> "retailer"
                    R.id.chipUser -> "user"
                    else -> "delivery"
                }
                loadCustomers()
            }
    }

    private fun loadCustomers() {

        swipeRefreshLayout.isRefreshing = true

        api.getCustomers(selectedType).enqueue(object : Callback<CustomerDashboardResponse> {

            override fun onResponse(
                call: Call<CustomerDashboardResponse>,
                response: Response<CustomerDashboardResponse>
            ) {
                if (isDestroyed) return
                swipeRefreshLayout.isRefreshing = false

                when {
                    response.isSuccessful -> {

                        allCustomers = response.body()?.customers ?: emptyList()

                        val currentQuery = etSearch.text.toString().trim()
                        filterCustomers(currentQuery)
                    }

                    response.code() == 401 -> {
                        sessionManager.logout()
                    }

                    else -> {
                        Toast.makeText(this@CustomersData, "Failed to load customers", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<CustomerDashboardResponse>, t: Throwable) {
                if (isDestroyed) return
                swipeRefreshLayout.isRefreshing = false

                Toast.makeText(
                    this@CustomersData,
                    "Please check your internet connection",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun filterCustomers(query: String) {

        val q = query.trim()

        val filtered = if (q.isEmpty()) allCustomers else
            allCustomers.filter { item ->
                listOfNotNull(item.name, item.shop_name, item.phone)
                    .any { it.contains(q, ignoreCase = true) }
            }

        adapter.update(filtered)

        showEmptyState(filtered.isEmpty())
    }

    private fun showEmptyState(show: Boolean) {
        tvEmptyState.visibility = if (show) View.VISIBLE else View.GONE
        rvCustomers.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun toggleFreeze(id: Int) {
        showScreenLoading()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.toggleFreeze(id)
                withContext(Dispatchers.Main) {
                    hideScreenLoading()
                    if (!isDestroyed) {
                        if (response.success) {
                            Toast.makeText(this@CustomersData, "Status updated", Toast.LENGTH_SHORT).show()
                            loadCustomers()
                        } else {
                            Toast.makeText(this@CustomersData, "Failed to update status", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideScreenLoading()
                    if (!isDestroyed) {
                        Toast.makeText(this@CustomersData, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showUpdateBalanceDialog(customer: CustomerItem) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.admin_customer_balance_update)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val txtName = dialog.findViewById<TextView>(R.id.txtCustomerName)
        val txtBalance = dialog.findViewById<TextView>(R.id.txtCurrentBalance)
        val txtRemainingDue = dialog.findViewById<TextView>(R.id.txtRemainingDue)
        val etAmount = dialog.findViewById<EditText>(R.id.etAmount)
        val btnUpdate = dialog.findViewById<MaterialButton>(R.id.btnUpdate)
        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnCancel)
        val currentDue = customer.due ?: 0.0

        txtName.text = customer.name
        txtBalance.text = "â‚¹ %.2f".format(currentDue)
        txtRemainingDue.text = "â‚¹ %.2f".format(currentDue)

        val dismissListener = View.OnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener(dismissListener)

        etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val paying = s.toString().toDoubleOrNull() ?: 0.0
                txtRemainingDue.text = "â‚¹ %.2f".format((currentDue - paying).coerceAtLeast(0.0))
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        btnUpdate.setOnClickListener {
            val amountStr = etAmount.text.toString().trim()
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Please enter amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            customer.id?.let { id ->
                updateBalance(id, amountStr, dialog)
            }
        }

        dialog.show()
    }

    private fun updateBalance(id: Int, amount: String, dialog: Dialog) {
        val btnUpdate = dialog.findViewById<MaterialButton>(R.id.btnUpdate)
        btnUpdate?.showLoading(true, "Updating...")
        showScreenLoading()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = UpdateBalanceRequest(amount = amount)
                val response = api.updateBalance(id, request)
                withContext(Dispatchers.Main) {
                    btnUpdate?.showLoading(false)
                    hideScreenLoading()
                    if (!isDestroyed) {
                        if (response.success) {
                            Toast.makeText(this@CustomersData, "Balance updated", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            loadCustomers()
                        } else {
                            Toast.makeText(this@CustomersData, response.message ?: "Failed to update balance", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnUpdate?.showLoading(false)
                    hideScreenLoading()
                    if (!isDestroyed) {
                        Toast.makeText(this@CustomersData, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
