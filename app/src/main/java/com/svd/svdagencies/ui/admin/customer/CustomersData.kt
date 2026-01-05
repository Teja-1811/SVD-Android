package com.svd.svdagencies.ui.admin.customer

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.admin.CustomerDashboardApi
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.CustomerDashboardResponse
import com.svd.svdagencies.data.model.admin.CustomerItem
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import com.svd.svdagencies.ui.auth.LoginActivity
import com.svd.svdagencies.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class CustomersData : AdminBaseActivity() {

    private lateinit var etSearch: EditText
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvCustomers: RecyclerView

    private lateinit var adapter: CustomersAdapter
    private var originalList: List<CustomerItem> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_customers)

        // Initialize Admin Drawer
        setupAdminLayout("Customers List")

        etSearch = findViewById(R.id.etSearch)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        rvCustomers = findViewById(R.id.rvCustomers)

        // Setup RecyclerView
        rvCustomers.layoutManager = LinearLayoutManager(this)
        adapter = CustomersAdapter()
        rvCustomers.adapter = adapter

        // Search Listener
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Swipe to Refresh
        swipeRefresh.setOnRefreshListener {
            loadCustomers()
        }

        loadCustomers()
    }

    private fun loadCustomers() {
        swipeRefresh.isRefreshing = true

        val session = SessionManager(this)
        val token = session.getToken()

        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
            return
        }

        // Use ApiClient.retrofit directly to ensure shared configuration is used
        val api = ApiClient.retrofit.create(CustomerDashboardApi::class.java)

        api.getCustomers("Token $token").enqueue(object : Callback<CustomerDashboardResponse> {
            override fun onResponse(
                call: Call<CustomerDashboardResponse>,
                response: Response<CustomerDashboardResponse>
            ) {
                swipeRefresh.isRefreshing = false
                if (response.isSuccessful && response.body() != null) {
                    originalList = response.body()!!.customers
                    adapter.setData(originalList)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("CustomersData", "Error Code: ${response.code()}, Body: $errorBody")
                    
                    if (response.code() == 403) {
                         Toast.makeText(this@CustomersData, "Access Denied (403). Check Permissions.", Toast.LENGTH_LONG).show()
                    } else {
                         Toast.makeText(this@CustomersData, "Server error ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<CustomerDashboardResponse>, t: Throwable) {
                swipeRefresh.isRefreshing = false
                Log.e("CustomersData", "Failure: ${t.localizedMessage}")
                Toast.makeText(this@CustomersData, "Network Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filter(query: String) {
        val lowerCaseQuery = query.lowercase(Locale.getDefault())
        val filteredList = originalList.filter {
            (it.name ?: "").lowercase(Locale.getDefault()).contains(lowerCaseQuery) ||
            (it.shop_name ?: "").lowercase(Locale.getDefault()).contains(lowerCaseQuery) ||
            (it.phone ?: "").contains(lowerCaseQuery)
        }
        adapter.setData(filteredList)
    }

    // ================= ADAPTER CLASS =================
    inner class CustomersAdapter : RecyclerView.Adapter<CustomersAdapter.ViewHolder>() {

        private var list: List<CustomerItem> = ArrayList()

        fun setData(newList: List<CustomerItem>) {
            list = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_customer, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]

            holder.txtCustomerName.text = "${item.serial_no ?: 0}. ${item.name ?: ""}"
            holder.txtShopName.text = item.shop_name ?: ""
            holder.txtPhone.text = item.phone ?: ""
            holder.txtBalance.text = "₹ ${item.due ?: 0.0}"
        }

        override fun getItemCount(): Int = list.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val txtCustomerName: TextView = itemView.findViewById(R.id.txtCustomerName)
            val txtShopName: TextView = itemView.findViewById(R.id.txtShopName)
            val txtPhone: TextView = itemView.findViewById(R.id.txtPhone)
            val txtBalance: TextView = itemView.findViewById(R.id.txtBalance)
            val btnView: ImageView = itemView.findViewById(R.id.btnView)
            val btnPassword: ImageView = itemView.findViewById(R.id.btnPassword)
            val btnWhatsapp: ImageView = itemView.findViewById(R.id.btnWhatsapp)
            val btnPay: ImageView = itemView.findViewById(R.id.btnPay)
        }
    }
}
