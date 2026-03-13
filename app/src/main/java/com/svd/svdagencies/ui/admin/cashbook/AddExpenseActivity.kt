package com.svd.svdagencies.ui.admin.cashbook

import android.app.Activity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.Cashbook.ExpenseRequest
import com.svd.svdagencies.databinding.AdminExpencesAddBinding
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import com.svd.svdagencies.utils.NetworkMessageUtils
import kotlinx.coroutines.launch

class AddExpenseActivity : AdminBaseActivity() {

    private lateinit var binding: AdminExpencesAddBinding
    private var expenseId: Int? = null

    private val categories = listOf("Fuel", "Rent", "Salary", "Electricity", "Repair", "Other")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdminExpencesAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAdminLayout("Add Expense")

        // Check if editing
        expenseId = intent.getIntExtra("EXPENSE_ID", -1).takeIf { it != -1 }
        if (expenseId != null) {
            binding.tvHeaderTitle.text = "Edit Expense"
            binding.btnSaveExpense.text = "Update Expense"
            loadExpenseDetails()
        }

        setupCategorySpinner()
        setupListeners()
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.spinnerCategory.setAdapter(adapter)
    }

    private fun setupListeners() {
        binding.btnSaveExpense.setOnClickListener {
            saveExpense()
        }
    }

    private fun loadExpenseDetails() {
        // Pre-fill data if passed via intent or fetch from API
        val amount = intent.getDoubleExtra("AMOUNT", 0.0)
        val category = intent.getStringExtra("CATEGORY")
        val description = intent.getStringExtra("DESCRIPTION")

        binding.etAmount.setText(amount.toString())
        binding.spinnerCategory.setText(category, false)
        binding.etDescription.setText(description)
    }

    private fun saveExpense() {
        val amount = binding.etAmount.text.toString().toDoubleOrNull()
        val category = binding.spinnerCategory.text.toString()
        val description = binding.etDescription.text.toString()

        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        if (category.isEmpty() || category == "Select Category") {
            Toast.makeText(this, "Select a category", Toast.LENGTH_SHORT).show()
            return
        }

        val request = ExpenseRequest(amount, category, description)
        binding.btnSaveExpense.isEnabled = false
        showScreenLoading()

        lifecycleScope.launch {
            try {
                val response = if (expenseId != null) {
                    ApiClient.cashbookApi.editExpense(expenseId!!, request)
                } else {
                    ApiClient.cashbookApi.addExpense(request)
                }

                if (response["success"] == true) {
                    setResult(Activity.RESULT_OK)
                    Toast.makeText(this@AddExpenseActivity, "Expense saved successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    binding.btnSaveExpense.isEnabled = true
                    hideScreenLoading()
                    Toast.makeText(this@AddExpenseActivity, "Failed to save expense", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.btnSaveExpense.isEnabled = true
                hideScreenLoading()
                Toast.makeText(
                    this@AddExpenseActivity,
                    NetworkMessageUtils.friendlyMessage(e, "Failed to save expense"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
