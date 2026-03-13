package com.svd.svdagencies.ui.admin

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.CustomerPaymentItem
import com.svd.svdagencies.databinding.AdminCustomerPaymentBinding
import com.svd.svdagencies.ui.admin.adapter.CustomerPaymentAdapter
import com.svd.svdagencies.utils.NetworkMessageUtils
import kotlinx.coroutines.launch

class AdminPaymentsActivity : AdminBaseActivity() {

    private lateinit var binding: AdminCustomerPaymentBinding
    private lateinit var adapter: CustomerPaymentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdminCustomerPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAdminLayout("Customer Payments")
        setupRecyclerView()
        setupListeners()
        
        fetchPayments()
    }

    private fun setupRecyclerView() {
        adapter = CustomerPaymentAdapter(
            onUpdateStatus = { payment -> showUpdateStatusDialog(payment) },
            onDelete = { payment -> showDeleteConfirmDialog(payment) }
        )
        binding.rvPayments.layoutManager = LinearLayoutManager(this)
        binding.rvPayments.adapter = adapter
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            fetchPayments()
        }
        
        binding.btnFilter.setOnClickListener {
            val customer = binding.etFilterCustomer.text.toString().trim()
            val txnId = binding.etFilterTxn.text.toString().trim()
            fetchPayments(customer, txnId)
        }

        binding.btnClear.setOnClickListener {
            binding.etFilterCustomer.text?.clear()
            binding.etFilterTxn.text?.clear()
            fetchPayments()
        }
    }

    private fun fetchPayments(customer: String? = null, transactionId: String? = null) {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val response = ApiClient.adminPaymentsApi.getCustomerPayments(
                    customer = if (customer.isNullOrEmpty()) null else customer,
                    transactionId = if (transactionId.isNullOrEmpty()) null else transactionId
                )
                adapter.setData(response.payments)
            } catch (e: Exception) {
                Toast.makeText(this@AdminPaymentsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showUpdateStatusDialog(payment: CustomerPaymentItem) {
        val statuses = arrayOf("PENDING", "SUCCESS", "FAILED")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Update Status for ${payment.transaction_id}")
        
        builder.setItems(statuses) { dialog, which ->
            val newStatus = statuses[which]
            updateStatus(payment.id, newStatus)
        }
        builder.show()
    }

    private fun updateStatus(paymentId: Int, status: String) {
        showScreenLoading()
        lifecycleScope.launch {
            try {
                val body = mapOf("status" to status)
                val response = ApiClient.adminPaymentsApi.updatePaymentStatus(paymentId, body)
                if (response["status"] == "success") {
                    hideScreenLoading()
                    Toast.makeText(this@AdminPaymentsActivity, "Status updated", Toast.LENGTH_SHORT).show()
                    fetchPayments()
                } else {
                    hideScreenLoading()
                    Toast.makeText(this@AdminPaymentsActivity, "Failed to update: ${response["message"]}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                hideScreenLoading()
                Toast.makeText(
                    this@AdminPaymentsActivity,
                    NetworkMessageUtils.friendlyMessage(e, "Failed to update payment"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showDeleteConfirmDialog(payment: CustomerPaymentItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Payment")
            .setMessage("Are you sure you want to delete transaction ${payment.transaction_id}?")
            .setPositiveButton("Delete") { _, _ ->
                deletePayment(payment.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePayment(paymentId: Int) {
        showScreenLoading()
        lifecycleScope.launch {
            try {
                val response = ApiClient.adminPaymentsApi.deletePayment(paymentId)
                if (response["status"] == "success") {
                    hideScreenLoading()
                    Toast.makeText(this@AdminPaymentsActivity, "Payment deleted", Toast.LENGTH_SHORT).show()
                    fetchPayments()
                } else {
                    hideScreenLoading()
                    Toast.makeText(this@AdminPaymentsActivity, "Failed to delete: ${response["message"]}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                hideScreenLoading()
                Toast.makeText(
                    this@AdminPaymentsActivity,
                    NetworkMessageUtils.friendlyMessage(e, "Failed to delete payment"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
