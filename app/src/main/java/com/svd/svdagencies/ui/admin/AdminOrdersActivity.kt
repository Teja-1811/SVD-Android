package com.svd.svdagencies.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.admin.AdminOrdersApi
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.Orders.AdminOrder
import com.svd.svdagencies.data.model.admin.Orders.ConfirmOrderItem
import com.svd.svdagencies.data.model.admin.Orders.ConfirmOrderRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AdminOrdersActivity : AdminBaseActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvOrders: RecyclerView
    private lateinit var layoutNoOrders: LinearLayout
    private lateinit var tvOrderCount: TextView
    private lateinit var api: AdminOrdersApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_orders_dashboard)

        setupAdminLayout("Orders")
        api = ApiClient.adminOrdersApi

        swipeRefresh = findViewById(R.id.swipeRefresh)
        rvOrders = findViewById(R.id.rvOrders)
        layoutNoOrders = findViewById(R.id.layoutNoOrders)
        tvOrderCount = findViewById(R.id.tvOrderCount)

        rvOrders.layoutManager = LinearLayoutManager(this)

        swipeRefresh.setOnRefreshListener {
            loadOrders()
        }

        loadOrders()
    }

    private fun loadOrders() {
        swipeRefresh.isRefreshing = true
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.getOrdersDashboard()
                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        swipeRefresh.isRefreshing = false
                        tvOrderCount.text = response.total_pending.toString()
                        
                        if (response.orders.isEmpty()) {
                            layoutNoOrders.visibility = View.VISIBLE
                            swipeRefresh.visibility = View.GONE
                        } else {
                            layoutNoOrders.visibility = View.GONE
                            swipeRefresh.visibility = View.VISIBLE
                            rvOrders.adapter = AdminOrdersAdapter(response.orders)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        swipeRefresh.isRefreshing = false
                        Toast.makeText(this@AdminOrdersActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private inner class AdminOrdersAdapter(private val orders: List<AdminOrder>) :
        RecyclerView.Adapter<AdminOrdersAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvOrderId: TextView = view.findViewById(R.id.tvOrderId)
            val tvCustomerName: TextView = view.findViewById(R.id.tvCustomerName)
            val tvOrderDate: TextView = view.findViewById(R.id.tvOrderDate)
            val layoutItems: LinearLayout = view.findViewById(R.id.layoutItems)
            val btnConfirm: MaterialButton = view.findViewById(R.id.btnConfirm)
            val btnReject: MaterialButton = view.findViewById(R.id.btnReject)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.admin_order_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val order = orders[position]
            holder.tvOrderId.text = "ORD-${order.order_id}"
            holder.tvCustomerName.text = "Customer: ${order.customer_name}"
            holder.tvOrderDate.text = "Order Date: ${order.order_date}"
            holder.btnConfirm.isEnabled = true
            holder.btnReject.isEnabled = true

            // Populate Items
            holder.layoutItems.removeAllViews()
            order.items?.forEach { item ->
                val row = LayoutInflater.from(holder.itemView.context)
                    .inflate(R.layout.admin_order_product, holder.layoutItems, false)
                
                row.findViewById<TextView>(R.id.tvItemName).text = item.item_name
                val etQty = row.findViewById<EditText>(R.id.etQty)
                val etDisc = row.findViewById<EditText>(R.id.etDisc)
                val tvAQ = row.findViewById<TextView>(R.id.tvAQ)
                
                etQty.setText(item.requested_quantity.toString())
                etDisc.setText(item.discount_per_qty.toString())
                tvAQ.text = item.available_quantity.toString()
                
                holder.layoutItems.addView(row)
            }

            holder.btnConfirm.setOnClickListener {
                confirmOrder(order, holder)
            }

            holder.btnReject.setOnClickListener {
                rejectOrder(order.order_id, holder)
            }
        }

        override fun getItemCount() = orders.size

        private fun confirmOrder(order: AdminOrder, holder: ViewHolder) {
            if (!holder.btnConfirm.isEnabled) return

            val confirmItems = mutableListOf<ConfirmOrderItem>()
            
            for (i in 0 until holder.layoutItems.childCount) {
                val row = holder.layoutItems.getChildAt(i)
                val item = order.items?.getOrNull(i) ?: continue
                
                val qty = row.findViewById<EditText>(R.id.etQty).text.toString().toIntOrNull() ?: 0
                val disc = row.findViewById<EditText>(R.id.etDisc).text.toString().toDoubleOrNull() ?: 0.0
                
                confirmItems.add(ConfirmOrderItem(item.item_id, qty, disc))
            }

            val request = ConfirmOrderRequest(confirmItems)
            holder.btnConfirm.isEnabled = false
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = api.confirmOrder(order.order_id, request)
                    withContext(Dispatchers.Main) {
                        val body = response.body()
                        val message = body?.message
                            ?: body?.error
                            ?: response.errorBody()?.string()?.extractApiMessage()
                            ?: "Confirm failed"

                        Toast.makeText(this@AdminOrdersActivity, message, Toast.LENGTH_SHORT).show()
                        if (response.isSuccessful && body?.success == true) {
                            loadOrders()
                        } else {
                            holder.btnConfirm.isEnabled = true
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        holder.btnConfirm.isEnabled = true
                        Toast.makeText(this@AdminOrdersActivity, "Confirm failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        private fun String.extractApiMessage(): String? {
            return runCatching {
                val json = JSONObject(this)
                json.optString("message")
                    .ifBlank { json.optString("error") }
                    .ifBlank { null }
            }.getOrNull()
        }

        private fun rejectOrder(orderId: Int, holder: ViewHolder) {
            if (!holder.btnReject.isEnabled) return
            holder.btnReject.isEnabled = false

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = api.rejectOrder(orderId)
                    withContext(Dispatchers.Main) {
                        val message = response.body()?.get("message")?.toString()
                            ?: response.body()?.get("error")?.toString()
                            ?: response.errorBody()?.string()?.extractApiMessage()
                            ?: "Reject failed"

                        Toast.makeText(this@AdminOrdersActivity, message, Toast.LENGTH_SHORT).show()
                        val success = response.body()?.get("success") as? Boolean
                        if (response.isSuccessful && success != false) {
                            loadOrders()
                        } else {
                            holder.btnReject.isEnabled = true
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        holder.btnReject.isEnabled = true
                        Toast.makeText(this@AdminOrdersActivity, "Reject failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
