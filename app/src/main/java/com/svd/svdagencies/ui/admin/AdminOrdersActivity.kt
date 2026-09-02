package com.svd.svdagencies.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.admin.AdminOrdersApi
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.Orders.AdminOrder
import com.svd.svdagencies.data.model.admin.Orders.ConfirmOrderItem
import com.svd.svdagencies.data.model.admin.Orders.ConfirmOrderRequest
import com.svd.svdagencies.databinding.AdminOrderCardBinding
import com.svd.svdagencies.databinding.AdminOrderProductBinding
import com.svd.svdagencies.databinding.AdminOrdersDashboardBinding
import com.svd.svdagencies.utils.showLoading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AdminOrdersActivity : AdminBaseActivity() {

    private lateinit var binding: AdminOrdersDashboardBinding
    private lateinit var api: AdminOrdersApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdminOrdersDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAdminLayout(getString(R.string.title_admin_orders))
        api = ApiClient.adminOrdersApi

        binding.rvOrders.layoutManager = LinearLayoutManager(this)

        binding.swipeRefresh.setOnRefreshListener {
            loadOrders()
        }

        loadOrders()
    }

    override fun onResume() {
        super.onResume()
        loadOrders()
    }

    private fun loadOrders() {
        binding.swipeRefresh.isRefreshing = true
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.getOrdersDashboard()
                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        binding.swipeRefresh.isRefreshing = false
                        binding.tvOrderCount.text = response.total_pending.toString()
                        
                        if (response.orders.isEmpty()) {
                            binding.layoutNoOrders.visibility = View.VISIBLE
                            binding.swipeRefresh.visibility = View.GONE
                        } else {
                            binding.layoutNoOrders.visibility = View.GONE
                            binding.swipeRefresh.visibility = View.VISIBLE
                            binding.rvOrders.adapter = AdminOrdersAdapter(response.orders)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        binding.swipeRefresh.isRefreshing = false
                        Toast.makeText(this@AdminOrdersActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private inner class AdminOrdersAdapter(private val orders: List<AdminOrder>) :
        RecyclerView.Adapter<AdminOrdersAdapter.ViewHolder>() {

        inner class ViewHolder(val cardBinding: AdminOrderCardBinding) : 
            RecyclerView.ViewHolder(cardBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val cardBinding = AdminOrderCardBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(cardBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val order = orders[position]
            val card = holder.cardBinding
            
            card.tvOrderId.text = getString(R.string.format_order_id, order.order_id)
            card.tvCustomerName.text = getString(R.string.format_customer_name, order.customer_name)
            card.tvOrderDate.text = getString(R.string.format_order_date, order.order_date)
            card.btnConfirm.isEnabled = true
            card.btnReject.isEnabled = true

            // Populate Items
            card.layoutItems.removeAllViews()
            order.items?.forEach { item ->
                val itemBinding = AdminOrderProductBinding.inflate(
                    LayoutInflater.from(holder.itemView.context), card.layoutItems, false
                )
                
                itemBinding.tvItemName.text = item.item_name
                itemBinding.etQty.setText(item.requested_quantity.toString())
                itemBinding.etDisc.setText(item.discount_per_qty.toString())
                itemBinding.tvAQ.text = item.available_quantity.toString()
                
                card.layoutItems.addView(itemBinding.root)
            }

            card.btnConfirm.setOnClickListener {
                confirmOrder(order, holder)
            }

            card.btnReject.setOnClickListener {
                rejectOrder(order.order_id, holder)
            }
        }

        override fun getItemCount() = orders.size

        private fun confirmOrder(order: AdminOrder, holder: ViewHolder) {
            val card = holder.cardBinding
            if (!card.btnConfirm.isEnabled) return

            val confirmItems = mutableListOf<ConfirmOrderItem>()
            
            for (i in 0 until card.layoutItems.childCount) {
                val row = card.layoutItems.getChildAt(i)
                val item = order.items?.getOrNull(i) ?: continue
                
                // Since we use addView with itemBinding.root, we can find the views inside the row
                val qty = row.findViewById<EditText>(R.id.etQty).text.toString().toIntOrNull() ?: 0
                val disc = row.findViewById<EditText>(R.id.etDisc).text.toString().toDoubleOrNull() ?: 0.0
                
                confirmItems.add(ConfirmOrderItem(item.item_id, qty, disc))
            }

            val request = ConfirmOrderRequest(confirmItems)
            card.btnConfirm.showLoading(true, "Confirming...")
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = api.confirmOrder(order.order_id, request)
                    withContext(Dispatchers.Main) {
                        card.btnConfirm.showLoading(false)
                        val body = response.body()
                        val message = body?.message
                            ?: body?.error
                            ?: response.errorBody()?.string()?.extractApiMessage()
                            ?: "Confirm failed"

                        Toast.makeText(this@AdminOrdersActivity, message, Toast.LENGTH_SHORT).show()
                        if (response.isSuccessful && body?.success == true) {
                            loadOrders()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        card.btnConfirm.showLoading(false)
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
            val card = holder.cardBinding
            if (!card.btnReject.isEnabled) return
            card.btnReject.showLoading(true, "Rejecting...")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = api.rejectOrder(orderId)
                    withContext(Dispatchers.Main) {
                        card.btnReject.showLoading(false)
                        val message = response.body()?.get("message")?.toString()
                            ?: response.body()?.get("error")?.toString()
                            ?: response.errorBody()?.string()?.extractApiMessage()
                            ?: "Reject failed"

                        Toast.makeText(this@AdminOrdersActivity, message, Toast.LENGTH_SHORT).show()
                        val success = response.body()?.get("success") as? Boolean
                        if (response.isSuccessful && success != false) {
                            loadOrders()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        card.btnReject.showLoading(false)
                        Toast.makeText(this@AdminOrdersActivity, "Reject failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
