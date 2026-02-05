package com.svd.svdagencies.ui.admin.cashbook

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.Cashbook.Expense
import com.svd.svdagencies.databinding.AdminViewExpencesBinding
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ViewExpensesActivity : AdminBaseActivity() {

    private lateinit var binding: AdminViewExpencesBinding
    private lateinit var adapter: ExpenseAdapter
    private var startDate: String? = null
    private var endDate: String? = null

    private val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    private var selectedMonth: Int? = null
    private var selectedYear: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdminViewExpencesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAdminLayout("Expense History")
        initViews()
        setupFilters()
        
        loadExpenses()
    }

    private fun initViews() {
        adapter = ExpenseAdapter(
            items = emptyList(),
            onEdit = { expense -> editExpense(expense) },
            onDelete = { expense -> confirmDelete(expense) }
        )
        binding.rvExpenses.apply {
            layoutManager = LinearLayoutManager(this@ViewExpensesActivity)
            this.adapter = this@ViewExpensesActivity.adapter
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadExpenses()
        }

        binding.btnApplyFilters.setOnClickListener {
            updateDatesFromFilters()
            loadExpenses()
        }
    }

    private fun setupFilters() {
        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, months)
        binding.spinnerMonth.setAdapter(monthAdapter)

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear - 2..currentYear + 1).map { it.toString() }
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, years)
        binding.spinnerYear.setAdapter(yearAdapter)

        val calendar = Calendar.getInstance()
        val currentMonthIdx = calendar.get(Calendar.MONTH)
        binding.spinnerMonth.setText(months[currentMonthIdx], false)
        binding.spinnerYear.setText(currentYear.toString(), false)
        
        selectedMonth = currentMonthIdx + 1
        selectedYear = currentYear
        
        updateDatesFromFilters()

        binding.spinnerMonth.setOnItemClickListener { _, _, position, _ ->
            selectedMonth = position + 1
        }

        binding.spinnerYear.setOnItemClickListener { _, _, position, _ ->
            selectedYear = years[position].toInt()
        }
    }

    private fun updateDatesFromFilters() {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        selectedYear?.let { calendar.set(Calendar.YEAR, it) }
        selectedMonth?.let { calendar.set(Calendar.MONTH, it - 1) }
        
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        startDate = sdf.format(calendar.time)
        
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        endDate = sdf.format(calendar.time)
    }

    private fun loadExpenses() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val response = ApiClient.cashbookApi.getExpenses(startDate, endDate)
                adapter.update(response.expenses)
                binding.tvTotalExpenses.text = "₹ %.2f".format(response.total_expenses)
            } catch (e: Exception) {
                if (!isFinishing) {
                    Toast.makeText(this@ViewExpensesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun editExpense(expense: Expense) {
        val intent = Intent(this, AddExpenseActivity::class.java).apply {
            putExtra("EXPENSE_ID", expense.id)
            putExtra("AMOUNT", expense.amount)
            putExtra("CATEGORY", expense.category)
            putExtra("DESCRIPTION", expense.description)
        }
        startActivity(intent)
    }

    private fun confirmDelete(expense: Expense) {
        AlertDialog.Builder(this)
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete this expense?")
            .setPositiveButton("Delete") { _, _ ->
                deleteExpense(expense.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteExpense(id: Int) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.cashbookApi.deleteExpense(id)
                if (response["success"] == true) {
                    Toast.makeText(this@ViewExpensesActivity, "Expense deleted", Toast.LENGTH_SHORT).show()
                    loadExpenses()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ViewExpensesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadExpenses()
    }
}