package com.svd.svdagencies.ui.delivery

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.svd.svdagencies.base.BaseActivity
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.databinding.DeliveryStockHistoryBinding
import com.svd.svdagencies.ui.delivery.adapter.DeliveryStockHistoryAdapter
import com.svd.svdagencies.utils.SessionManager
import kotlinx.coroutines.launch
import retrofit2.awaitResponse

class DeliveryStockHistoryActivity : BaseActivity() {

    private lateinit var binding: DeliveryStockHistoryBinding
    private lateinit var adapter: DeliveryStockHistoryAdapter
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DeliveryStockHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        
        loadHistory()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = DeliveryStockHistoryAdapter { item ->
            // Return to entry activity with selected date
            val intent = Intent().apply {
                putExtra("selected_date", item.date)
            }
            setResult(RESULT_OK, intent)
            finish()
        }
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener { loadHistory() }
    }

    private fun loadHistory() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                val response = ApiClient.deliveryApi.getDeliveryHistory(
                    agentId = session.getUserId()
                ).awaitResponse()
                
                if (response.isSuccessful) {
                    val history = response.body()?.history ?: emptyList()
                    adapter.submitList(history)
                    binding.tvEmpty.visibility = if (history.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    Toast.makeText(this@DeliveryStockHistoryActivity, "Failed to load history", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DeliveryStockHistoryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
}
