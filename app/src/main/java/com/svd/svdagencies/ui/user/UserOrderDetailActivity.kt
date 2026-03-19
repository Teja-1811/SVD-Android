package com.svd.svdagencies.ui.user

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.api.user.UserApi
import com.svd.svdagencies.data.model.user.UserOrderDetailResponse
import com.svd.svdagencies.ui.user.adapter.UserOrderItemAdapter
import com.svd.svdagencies.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserOrderDetailActivity : AppCompatActivity() {

    private lateinit var tvOrderNum: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvDeliveryDate: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvSubtotal: TextView
    private lateinit var tvDeliveryCharge: TextView
    private lateinit var tvTotal: TextView
    private lateinit var rvItems: RecyclerView

    private lateinit var sessionManager: SessionManager
    private lateinit var api: UserApi
    private var orderId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_order_details)

        orderId = intent.getIntExtra("ORDER_ID", -1)
        if (orderId == -1) {
            Toast.makeText(this, "Invalid Order ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        sessionManager = SessionManager(this)
        api = ApiClient.retrofit.create(UserApi::class.java)

        initViews()
        loadOrderDetails()
    }

    private fun initViews() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        tvOrderNum = findViewById(R.id.tvOrderNumber)
        tvStatus = findViewById(R.id.tvOrderStatus)
        tvDate = findViewById(R.id.tvOrderDate)
        tvDeliveryDate = findViewById(R.id.tvDeliveryDate)
        tvAddress = findViewById(R.id.tvDeliveryAddress)
        tvSubtotal = findViewById(R.id.tvSubtotal)
        tvDeliveryCharge = findViewById(R.id.tvDeliveryCharge)
        tvTotal = findViewById(R.id.tvOrderTotal)
        rvItems = findViewById(R.id.rvOrderItems)

        rvItems.layoutManager = LinearLayoutManager(this)
    }

    private fun loadOrderDetails() {
        val userId = sessionManager.getUserId()
        api.getUserOrderDetail(orderId, userId).enqueue(object : Callback<UserOrderDetailResponse> {
            override fun onResponse(call: Call<UserOrderDetailResponse>, response: Response<UserOrderDetailResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    displayOrder(response.body()!!)
                } else {
                    Toast.makeText(this@UserOrderDetailActivity, "Failed to load order details", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserOrderDetailResponse>, t: Throwable) {
                Toast.makeText(this@UserOrderDetailActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayOrder(data: UserOrderDetailResponse) {
        val order = data.order
        tvOrderNum.text = "Order #${order.orderNumber}"
        tvStatus.text = "Status: ${order.status}"
        tvDate.text = "Ordered On: ${order.orderDate}"
        tvDeliveryDate.text = "Delivery Date: ${order.deliveryDate ?: "TBA"}"
        tvAddress.text = order.deliveryAddress ?: "No address provided"
        
        tvSubtotal.text = "₹%.2f".format(order.totalAmount)
        tvDeliveryCharge.text = "₹%.2f".format(order.deliveryCharge)
        
        // Use approved total if available
        val total = if (order.approvedTotalAmount > 0) order.approvedTotalAmount else (order.totalAmount + order.deliveryCharge)
        tvTotal.text = "₹%.2f".format(total)

        rvItems.adapter = UserOrderItemAdapter(data.items)
    }
}
