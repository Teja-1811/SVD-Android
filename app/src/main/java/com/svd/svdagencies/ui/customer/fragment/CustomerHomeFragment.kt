package com.svd.svdagencies.ui.customer.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.api.customer.CustomerApi
import com.svd.svdagencies.data.model.customer.CustomerDashboardResponse
import com.svd.svdagencies.utils.Refreshable
import com.svd.svdagencies.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CustomerHomeFragment :
    Fragment(R.layout.customer_home),
    Refreshable {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvWelcome: TextView
    private lateinit var tvBalance: TextView
    private lateinit var tvShop: TextView
    private lateinit var tvStatus: TextView

    private lateinit var api: CustomerApi
    private lateinit var session: SessionManager
    private var userId: Int = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvWelcome = view.findViewById(R.id.tvWelcome)
        tvBalance = view.findViewById(R.id.tvBalance)
        tvShop = view.findViewById(R.id.tvShop)
        tvStatus = view.findViewById(R.id.tvStatus)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        // Session
        session = SessionManager(requireContext())
        userId = session.getUserId()

        if (userId == -1) {
            Toast.makeText(requireContext(), "Session expired", Toast.LENGTH_SHORT).show()
            return
        }

        // API
        api = ApiClient.retrofit.create(CustomerApi::class.java)
        
        swipeRefresh.setOnRefreshListener {
            loadDashboard()
        }

        // Initial load
        swipeRefresh.post {
            swipeRefresh.isRefreshing = true
            loadDashboard()
        }
    }

    // ================= LOAD DASHBOARD =================
    private fun loadDashboard() {

        api.getDashboard(userId).enqueue(object : Callback<CustomerDashboardResponse> {

            override fun onResponse(
                call: Call<CustomerDashboardResponse>,
                response: Response<CustomerDashboardResponse>
            ) {
                val context = context ?: return // Safe check
                
                swipeRefresh.isRefreshing = false
                
                if (!response.isSuccessful) {
                    Toast.makeText(
                        context,
                        "Server error: ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                Log.d("CustomerHomeFragment", "Response: ${response.body()}")

                val customer = response.body() ?: return

                tvWelcome.text = "Welcome back, ${customer.customerName} 👋"
                tvShop.text = customer.shopName
                tvBalance.text = "₹ ${customer.balance}"

                if (customer.accountStatus == "Active") {
                    tvStatus.text = "Active"
                    tvStatus.setTextColor(
                        ContextCompat.getColor(context, R.color.icon_green)
                    )
                } else {
                    tvStatus.text = "Inactive"
                    tvStatus.setTextColor(
                        ContextCompat.getColor(context, R.color.login_text_red)
                    )
                }
            }

            override fun onFailure(call: Call<CustomerDashboardResponse>, t: Throwable) {
                val context = context ?: return // Safe check
                
                swipeRefresh.isRefreshing = false
                Toast.makeText(
                    context,
                    "Network error: ${t.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    // ================= PULL TO REFRESH =================
    override fun onRefresh() {
        loadDashboard()
    }
}
