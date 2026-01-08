package com.svd.svdagencies.ui.admin.bills

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.AdminBill
import com.svd.svdagencies.data.model.admin.BillListResponse
import com.svd.svdagencies.data.model.admin.CustomerDashboardResponse
import com.svd.svdagencies.data.model.admin.CustomerItem
import com.svd.svdagencies.ui.admin.Adapter.AdminBillAdapter
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import com.svd.svdagencies.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class AdminBillsActivity : AdminBaseActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvBills: RecyclerView
    private lateinit var spinnerCustomers: Spinner
    private lateinit var tvFromDate: TextView
    private lateinit var tvToDate: TextView
    private lateinit var btnSearch: MaterialButton
    private lateinit var btnClear: MaterialButton
    private lateinit var btnAddBill: MaterialCardView

    private lateinit var adapter: AdminBillAdapter
    private var allCustomers: List<CustomerItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_bills)

        setupAdminLayout("Bills")

        // Views
        swipeRefresh = findViewById(R.id.swipeRefresh)
        rvBills = findViewById(R.id.rvBills)
        spinnerCustomers = findViewById(R.id.spinnerCustomers)
        tvFromDate = findViewById(R.id.tvFromDate)
        tvToDate = findViewById(R.id.tvToDate)
        btnSearch = findViewById(R.id.btnSearch)
        btnClear = findViewById(R.id.btnClear)
        btnAddBill = findViewById(R.id.btnAddBill)

        setupRecycler()
        setupListeners()
        loadCustomers()

        swipeRefresh.setOnRefreshListener {
            loadBills()
        }

        // Initially load data
        loadBills()
    }

    private fun setupRecycler() {
        adapter = AdminBillAdapter(
            emptyList(),
            onViewClick = { bill -> showToast("View Bill: ${bill.bill_number}") },
            onEditClick = { bill -> showToast("Edit Bill: ${bill.bill_number}") },
            onDownloadClick = { bill -> showToast("Download Bill: ${bill.bill_number}") },
            onDeleteClick = { bill -> showToast("Delete Bill: ${bill.bill_number}") }
        )
        rvBills.layoutManager = LinearLayoutManager(this)
        rvBills.adapter = adapter
    }

    private fun setupListeners() {
        tvFromDate.setOnClickListener { showDatePicker(tvFromDate) }
        tvToDate.setOnClickListener { showDatePicker(tvToDate) }

        btnSearch.setOnClickListener {
            loadBills()
        }

        btnClear.setOnClickListener {
            tvFromDate.text = "dd-mm-yyyy"
            tvToDate.text = "dd-mm-yyyy"
            if (spinnerCustomers.adapter != null && spinnerCustomers.count > 0) {
                spinnerCustomers.setSelection(0)
            }
            loadBills()
        }

        btnAddBill.setOnClickListener {
            showToast("Add Bill Clicked")
        }
    }

    private fun showDatePicker(textView: TextView) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate =
                    String.format("%d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                textView.text = formattedDate
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun loadCustomers() {
        val session = SessionManager(this)
        val token = session.getToken()

        if (token.isNullOrEmpty()) {
            showToast("Session expired. Please login again.")
            return
        }

        ApiClient.adminCustomerDashboard.getCustomers()
            .enqueue(object : Callback<CustomerDashboardResponse> {
                override fun onResponse(
                    call: Call<CustomerDashboardResponse>,
                    response: Response<CustomerDashboardResponse>
                ) {
                    if (response.isSuccessful) {
                        allCustomers = response.body()?.customers ?: emptyList()
                        setupCustomerSpinner()
                    }
                }

                override fun onFailure(call: Call<CustomerDashboardResponse>, t: Throwable) {
                    showToast("Failed to load customers")
                }
            })
    }

    private fun setupCustomerSpinner() {
        val customerNames = mutableListOf("All Customers")
        customerNames.addAll(allCustomers.map { it.name ?: "" })

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, customerNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCustomers.adapter = adapter
    }

    private fun loadBills() {
        swipeRefresh.isRefreshing = true

        val customerId = if (spinnerCustomers.adapter != null && spinnerCustomers.selectedItemPosition > 0) {
            allCustomers[spinnerCustomers.selectedItemPosition - 1].id
        } else {
            null
        }

        val startDate = if (tvFromDate.text.toString() != "dd-mm-yyyy") tvFromDate.text.toString() else null
        val endDate = if (tvToDate.text.toString() != "dd-mm-yyyy") tvToDate.text.toString() else null

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.billsDashboardApi.getBills(
                    customerId = customerId,
                    startDate = startDate,
                    endDate = endDate,
                    page = 1
                )
                
                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        swipeRefresh.isRefreshing = false
                        // Convert BillItem (API) to AdminBill (UI Model)
                        // Or update Adapter to use BillItem directly.
                        // For now mapping to AdminBill as Adapter expects it.
                        // Note: API BillItem fields: id, invoice_number, invoice_date, customer, total_amount, op_due, current_due
                        // UI AdminBill fields: id, bill_number, customer_name, date, total_amount, profit
                        
                        val uiBills = response.results.map { apiBill ->
                            AdminBill(
                                id = apiBill.id,
                                bill_number = apiBill.invoice_number,
                                customer_name = apiBill.customer,
                                date = apiBill.invoice_date,
                                total_amount = apiBill.total_amount.toDoubleOrNull() ?: 0.0,
                                profit = 0.0 // API list doesn't return profit, only detail does
                            )
                        }
                        adapter.updateList(uiBills)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        swipeRefresh.isRefreshing = false
                        showToast("Error loading bills: ${e.message}")
                    }
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
