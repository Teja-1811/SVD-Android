package com.svd.svdagencies.ui.admin

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.CustomerSubscription
import com.svd.svdagencies.data.model.admin.SubscriptionCustomer
import com.svd.svdagencies.data.model.admin.SubscriptionPlan
import com.svd.svdagencies.databinding.AdminSubscriptionHistoryBinding
import com.svd.svdagencies.ui.admin.adapter.PaymentHistoryAdapter
import com.svd.svdagencies.ui.admin.adapter.PaymentRecord
import com.svd.svdagencies.ui.admin.adapter.SubscriptionHistoryAdapter
import com.svd.svdagencies.utils.NetworkMessageUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminSubscriptionHistoryActivity : AdminBaseActivity() {

    private lateinit var binding: AdminSubscriptionHistoryBinding
    private lateinit var subAdapter: SubscriptionHistoryAdapter
    private lateinit var payAdapter: PaymentHistoryAdapter
    private val gson = Gson()

    private var allCustomers: List<SubscriptionCustomer> = emptyList()
    private var allPlans: List<SubscriptionPlan> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdminSubscriptionHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAdminLayout("Subscription History")
        setupRecyclerViews()
        setupFilters()
        
        binding.swipeRefresh.setOnRefreshListener {
            loadHistory()
        }

        loadFilters()
        loadHistory()
    }

    private fun setupRecyclerViews() {
        subAdapter = SubscriptionHistoryAdapter(emptyList())
        binding.rvSubscriptionHistory.layoutManager = LinearLayoutManager(this)
        binding.rvSubscriptionHistory.adapter = subAdapter

        // Using empty list for now as Payment API isn't explicitly defined for history yet
        payAdapter = PaymentHistoryAdapter(emptyList())
        binding.rvPaymentHistory.layoutManager = LinearLayoutManager(this)
        binding.rvPaymentHistory.adapter = payAdapter
    }

    private fun setupFilters() {
        binding.btnFilter.setOnClickListener {
            loadHistory()
        }
        binding.btnReset.setOnClickListener {
            binding.spinnerCustomer.setSelection(0)
            binding.spinnerPlan.setSelection(0)
            loadHistory()
        }
    }

    private fun loadFilters() {
        ApiClient.subscriptionApi.getSubscriptionCustomers().enqueue(object : Callback<List<SubscriptionCustomer>> {
            override fun onResponse(call: Call<List<SubscriptionCustomer>>, response: Response<List<SubscriptionCustomer>>) {
                if (response.isSuccessful) {
                    allCustomers = response.body() ?: emptyList()
                    val names = mutableListOf("All Customers")
                    names.addAll(allCustomers.map { it.name })
                    val adapter = ArrayAdapter(this@AdminSubscriptionHistoryActivity, android.R.layout.simple_spinner_item, names)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerCustomer.adapter = adapter
                }
            }
            override fun onFailure(call: Call<List<SubscriptionCustomer>>, t: Throwable) {}
        })

        ApiClient.subscriptionApi.getPlans().enqueue(object : Callback<List<SubscriptionPlan>> {
            override fun onResponse(call: Call<List<SubscriptionPlan>>, response: Response<List<SubscriptionPlan>>) {
                if (response.isSuccessful) {
                    allPlans = response.body() ?: emptyList()
                    val names = mutableListOf("All Plans")
                    names.addAll(allPlans.map { it.name })
                    val adapter = ArrayAdapter(this@AdminSubscriptionHistoryActivity, android.R.layout.simple_spinner_item, names)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerPlan.adapter = adapter
                }
            }
            override fun onFailure(call: Call<List<SubscriptionPlan>>, t: Throwable) {}
        })
    }

    private fun loadHistory() {
        binding.swipeRefresh.isRefreshing = true
        
        val customerId = if (binding.spinnerCustomer.selectedItemPosition > 0) {
            allCustomers[binding.spinnerCustomer.selectedItemPosition - 1].id
        } else null
        val planId = if (binding.spinnerPlan.selectedItemPosition > 0) {
            allPlans[binding.spinnerPlan.selectedItemPosition - 1].id
        } else null

        ApiClient.subscriptionApi.getSubscriptionHistory(customerId, planId).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                binding.swipeRefresh.isRefreshing = false
                if (response.isSuccessful) {
                    val body = response.body().orEmpty()
                    val subs = parseSubscriptions(body)
                    val payments = parsePayments(body)
                    subAdapter.updateData(subs)
                    payAdapter.updateData(payments)
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(
                    this@AdminSubscriptionHistoryActivity,
                    NetworkMessageUtils.friendlyMessage(t, "Failed to load history"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun parseSubscriptions(body: Map<String, Any>): List<CustomerSubscription> {
        val rawList = extractList(
            body,
            "subscriptions",
            "subscription_history",
            "history",
            "results"
        ) ?: return emptyList()

        return try {
            val json = gson.toJson(rawList)
            val type = object : TypeToken<List<CustomerSubscription>>() {}.type
            gson.fromJson(json, type)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parsePayments(body: Map<String, Any>): List<PaymentRecord> {
        val rawList = extractList(
            body,
            "payments",
            "payment_history",
            "payment_records"
        ) ?: return emptyList()

        val payments = mutableListOf<PaymentRecord>()
        for (item in rawList) {
            val entry = item as? Map<*, *> ?: continue
            val amount = entry.readDouble("amount", "paid_amount") ?: 0.0
            payments += PaymentRecord(
                customer = entry.readString("customer", "customer_name") ?: "Unknown",
                amount = amount,
                method = entry.readString("payment_method", "method") ?: "N/A",
                txnId = entry.readString("transaction_id", "txn_id", "reference_id"),
                date = entry.readString("payment_date", "date", "created_at") ?: "-"
            )
        }
        return payments
    }

    private fun extractList(body: Map<String, Any>, vararg keys: String): List<Any>? {
        for (key in keys) {
            val value = body[key]
            if (value is List<*>) {
                @Suppress("UNCHECKED_CAST")
                return value as List<Any>
            }
        }
        return null
    }

    private fun Map<*, *>.readString(vararg keys: String): String? {
        for (key in keys) {
            val value = this[key] ?: continue
            val text = value.toString().trim()
            if (text.isNotEmpty() && text.lowercase() != "null") {
                return text
            }
        }
        return null
    }

    private fun Map<*, *>.readDouble(vararg keys: String): Double? {
        for (key in keys) {
            val value = this[key] ?: continue
            when (value) {
                is Number -> return value.toDouble()
                is String -> value.toDoubleOrNull()?.let { return it }
            }
        }
        return null
    }
}
