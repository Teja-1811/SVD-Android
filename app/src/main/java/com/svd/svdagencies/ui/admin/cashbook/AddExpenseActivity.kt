package com.svd.svdagencies.ui.admin.cashbook

import android.app.Activity
import android.app.DatePickerDialog
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch

class AddExpenseActivity : AdminBaseActivity() {

    private lateinit var binding: AdminExpencesAddBinding
    private var expenseId: Int? = null

    private val categories = listOf("Ramesh", "Electricity", "Travel", "Bhanu")
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

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
        if (binding.etDate.text.isNullOrBlank()) {
            binding.etDate.setText(dateFormatter.format(Calendar.getInstance().time))
        }
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.spinnerCategory.setAdapter(adapter)
    }

    private fun setupListeners() {
        binding.btnSaveExpense.setOnClickListener {
            saveExpense()
        }
        binding.etDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun loadExpenseDetails() {
        // Pre-fill data if passed via intent or fetch from API
        val amount = intent.getDoubleExtra("AMOUNT", 0.0)
        val category = intent.getStringExtra("CATEGORY")
        val description = intent.getStringExtra("DESCRIPTION")
        val date = intent.getStringExtra("DATE")

        binding.etAmount.setText(amount.toString())
        binding.spinnerCategory.setText(category, false)
        binding.etDescription.setText(description)
        if (!date.isNullOrBlank()) {
            binding.etDate.setText(date)
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        runCatching {
            val parsed = dateFormatter.parse(binding.etDate.text.toString())
            if (parsed != null) calendar.time = parsed
        }

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                binding.etDate.setText(dateFormatter.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveExpense() {
        val amount = binding.etAmount.text.toString().toDoubleOrNull()
        val category = binding.spinnerCategory.text.toString()
        val description = binding.etDescription.text.toString()
        val expenseDate = binding.etDate.text.toString().trim()

        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        if (category.isEmpty() || category == "Select Category") {
            Toast.makeText(this, "Select a category", Toast.LENGTH_SHORT).show()
            return
        }

        if (category !in categories) {
            Toast.makeText(this, "Select a valid category", Toast.LENGTH_SHORT).show()
            return
        }

        if (expenseDate.isEmpty()) {
            Toast.makeText(this, "Select a date", Toast.LENGTH_SHORT).show()
            return
        }

        val request = ExpenseRequest(amount, category, description, expenseDate)
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
