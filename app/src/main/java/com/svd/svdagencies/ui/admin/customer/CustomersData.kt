package com.svd.svdagencies.ui.admin.customer

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.admin.CustomerDashboardApi
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.CustomerDashboardResponse
import com.svd.svdagencies.data.model.admin.CustomerItem
import com.svd.svdagencies.data.model.admin.UpdateBalanceRequest
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import com.svd.svdagencies.ui.admin.Adapter.CustomerAdapter
import com.svd.svdagencies.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CustomersData : AdminBaseActivity() {

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var rvCustomers: androidx.recyclerview.widget.RecyclerView
    private lateinit var adapter: CustomerAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var api: CustomerDashboardApi

    private lateinit var etSearch: EditText
    private lateinit var fabAddCustomer: FloatingActionButton
    private lateinit var tvEmptyState: TextView

    private var allCustomers: List<CustomerItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_customers)

        // Setup shared admin UI
        setupAdminLayout("Customers")

        sessionManager = SessionManager(this)
        api = ApiClient.adminCustomerDashboard

        initViews()
        setupListeners()

        swipeRefreshLayout.isRefreshing = true
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
            startActivity(intent)
        }
    }

    private fun loadCustomers() {

        swipeRefreshLayout.isRefreshing = true

        api.getCustomers().enqueue(object : Callback<CustomerDashboardResponse> {

            override fun onResponse(
                call: Call<CustomerDashboardResponse>,
                response: Response<CustomerDashboardResponse>
            ) {
                if (isDestroyed) return
                swipeRefreshLayout.isRefreshing = false

                when {
                    response.isSuccessful -> {

                        allCustomers = response.body()?.customers ?: emptyList()

                        adapter.update(allCustomers)

                        val currentQuery = etSearch.text.toString().trim()
                        if (currentQuery.isNotEmpty()) filterCustomers(currentQuery)

                        showEmptyState(allCustomers.isEmpty())
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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.toggleFreeze(id)
                withContext(Dispatchers.Main) {
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
        dialog.setContentView(R.layout.dialog_update_balance)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val txtName = dialog.findViewById<TextView>(R.id.txtCustomerName)
        val txtBalance = dialog.findViewById<TextView>(R.id.txtCurrentBalance)
        val etAmount = dialog.findViewById<EditText>(R.id.etAmount)
        val btnUpdate = dialog.findViewById<MaterialButton>(R.id.btnUpdate)
        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnCancel)
        val btnClose = dialog.findViewById<ImageView>(R.id.btnClose)

        txtName.text = customer.name
        txtBalance.text = "₹ %.2f".format(customer.due ?: 0.0)

        val dismissListener = View.OnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener(dismissListener)
        btnClose.setOnClickListener(dismissListener)

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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = UpdateBalanceRequest(amount = amount)
                val response = api.updateBalance(id, request)
                withContext(Dispatchers.Main) {
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
                    if (!isDestroyed) {
                        Toast.makeText(this@CustomersData, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
