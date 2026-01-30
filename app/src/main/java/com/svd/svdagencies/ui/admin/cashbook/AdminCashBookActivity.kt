package com.svd.svdagencies.ui.admin.cashbook

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.CashbookDashboardResponse
import com.svd.svdagencies.data.model.admin.SaveBankBalanceRequest
import com.svd.svdagencies.data.model.admin.SaveCashInRequest
import com.svd.svdagencies.databinding.AdminCashbookBinding
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import kotlinx.coroutines.launch

class AdminCashBookActivity : AdminBaseActivity() {

    private lateinit var binding: AdminCashbookBinding
    private lateinit var companyDueAdapter: CompanyDueAdapter

    private val noteValues = listOf(500, 200, 100, 50, 20, 10)
    private val coinValues = listOf(20, 10, 5, 2, 1)

    private val noteEditTexts = mutableMapOf<Int, EditText>()
    private val coinEditTexts = mutableMapOf<Int, EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdminCashbookBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAdminLayout("CashBook")
        initViews()
        setupCashInGrids()
        loadDashboardData()
    }

    private fun initViews() {
        // Initialize Stat Labels
        binding.statCash.tvStatLabel.text = "CASH IN HAND"
        binding.statBank.tvStatLabel.text = "BANK BALANCE"
        binding.statDues.tvStatLabel.text = "CUSTOMER DUES"
        binding.statNetCash.tvStatLabel.text = "NET CASH"
        binding.statExpenses.tvStatLabel.text = "MONTHLY EXPENSES"
        binding.statProfit.tvStatLabel.text = "MONTHLY PROFIT"
        binding.statStockValue.tvStatLabel.text = "STOCK VALUE"
        binding.statRAmount.tvStatLabel.text = "REMAINING AMOUNT"

        // Setup RecyclerView for Company Dues
        companyDueAdapter = CompanyDueAdapter(emptyList())
        binding.rvCompanyDues.apply {
            layoutManager = LinearLayoutManager(this@AdminCashBookActivity)
            adapter = companyDueAdapter
            isNestedScrollingEnabled = false // Important inside NestedScrollView
        }

        binding.btnUpdateBank.setOnClickListener { updateBankBalance() }
        binding.btnUpdateCashIn.setOnClickListener { updateCashIn() }
        
        binding.btnAddExpense.setOnClickListener {
            Toast.makeText(this, "Opening Add Expense", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnViewExpenses.setOnClickListener {
            Toast.makeText(this, "Opening Expense List", Toast.LENGTH_SHORT).show()
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadDashboardData()
        }
    }

    private fun setupCashInGrids() {
        binding.gridNotes.removeAllViews()
        binding.gridCoins.removeAllViews()
        noteEditTexts.clear()
        coinEditTexts.clear()

        // Create Notes Inputs
        for (value in noteValues) {
            val view = layoutInflater.inflate(R.layout.admin_cashbook_denomination_input, binding.gridNotes, false)
            val et = setupDenominationItem(view, value)
            noteEditTexts[value] = et
            binding.gridNotes.addView(view)
        }

        // Create Coins Inputs
        for (value in coinValues) {
            val view = layoutInflater.inflate(R.layout.admin_cashbook_denomination_input, binding.gridCoins, false)
            val et = setupDenominationItem(view, value)
            coinEditTexts[value] = et
            binding.gridCoins.addView(view)
        }
    }

    private fun setupDenominationItem(view: View, value: Int): EditText {
        val tvLabel = view.findViewById<TextView>(R.id.tvLabel)
        val etCount = view.findViewById<EditText>(R.id.etCount)
        val tvTotal = view.findViewById<TextView>(R.id.tvTotal)

        tvLabel.text = "₹$value"
        etCount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val countInt = s.toString().toIntOrNull() ?: 0
                tvTotal.text = "Total: ₹${countInt * value}"
                updateLiveCashTotal() 
            }
        })
        return etCount
    }

    private fun updateLiveCashTotal() {
        var total = 0.0
        noteEditTexts.forEach { (value, et) ->
            total += (et.text.toString().toIntOrNull() ?: 0) * value
        }
        coinEditTexts.forEach { (value, et) ->
            total += (et.text.toString().toIntOrNull() ?: 0) * value
        }
        binding.statCash.tvStatValue.text = "₹%.2f".format(total)
    }

    private fun loadDashboardData() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val response = ApiClient.cashbookApi.getDashboardData()
                populateUI(response)
            } catch (e: Exception) {
                if (!isFinishing) {
                    Toast.makeText(this@AdminCashBookActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun populateUI(data: CashbookDashboardResponse) {
        // Core Summary
        binding.statCash.tvStatValue.text = "₹%.2f".format(data.cash_in)
        binding.statBank.tvStatValue.text = "₹%.2f".format(data.bank_balance)
        binding.statDues.tvStatValue.text = "₹%.2f".format(data.total_customer_dues)
        binding.statNetCash.tvStatValue.text = "₹%.2f".format(data.net_cash)
        binding.statExpenses.tvStatValue.text = "₹%.2f".format(data.cash_out)
        binding.statProfit.tvStatValue.text = "₹%.2f".format(data.monthly_profit)
        binding.statStockValue.tvStatValue.text = "₹%.2f".format(data.stock_value)
        binding.statRAmount.tvStatValue.text = "₹%.2f".format(data.remaining_amount)
        binding.tvNetProfitValue.text = "₹%.2f".format(data.net_profit)

        binding.etBankBalance.setText(data.bank_balance.toString())

        // Company Dues - Now using Adapter
        companyDueAdapter.update(data.company_dues)
        binding.tvTotalCompanyDues.text = "Total Company Dues: ₹%.2f".format(data.total_company_dues)

        // Reflect Denominations from Database
        data.denominations.let { d ->
            setDenominationValue(noteEditTexts[500], d.c500)
            setDenominationValue(noteEditTexts[200], d.c200)
            setDenominationValue(noteEditTexts[100], d.c100)
            setDenominationValue(noteEditTexts[50], d.c50)
            setDenominationValue(noteEditTexts[20], d.c20)
            setDenominationValue(noteEditTexts[10], d.c10)

            setDenominationValue(coinEditTexts[20], d.coin20)
            setDenominationValue(coinEditTexts[10], d.coin10)
            setDenominationValue(coinEditTexts[5], d.coin5)
            setDenominationValue(coinEditTexts[2], d.coin2)
            setDenominationValue(coinEditTexts[1], d.coin1)
        }
    }

    private fun setDenominationValue(et: EditText?, value: Int) {
        if (et == null) return
        val currentText = et.text.toString()
        val newText = if (value == 0) "" else value.toString()
        if (currentText != newText) {
            et.setText(newText)
        }
    }

    private fun updateBankBalance() {
        val balance = binding.etBankBalance.text.toString().toDoubleOrNull() ?: return
        lifecycleScope.launch {
            try {
                ApiClient.cashbookApi.saveBankBalance(SaveBankBalanceRequest(balance))
                Toast.makeText(this@AdminCashBookActivity, "Bank balance updated", Toast.LENGTH_SHORT).show()
                loadDashboardData()
            } catch (e: Exception) {
                Toast.makeText(this@AdminCashBookActivity, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateCashIn() {
        val request = SaveCashInRequest(
            c500 = noteEditTexts[500]?.text.toString().toIntOrNull() ?: 0,
            c200 = noteEditTexts[200]?.text.toString().toIntOrNull() ?: 0,
            c100 = noteEditTexts[100]?.text.toString().toIntOrNull() ?: 0,
            c50 = noteEditTexts[50]?.text.toString().toIntOrNull() ?: 0,
            c20 = noteEditTexts[20]?.text.toString().toIntOrNull() ?: 0,
            c10 = noteEditTexts[10]?.text.toString().toIntOrNull() ?: 0,
            coin20 = coinEditTexts[20]?.text.toString().toIntOrNull() ?: 0,
            coin10 = coinEditTexts[10]?.text.toString().toIntOrNull() ?: 0,
            coin5 = coinEditTexts[5]?.text.toString().toIntOrNull() ?: 0,
            coin2 = coinEditTexts[2]?.text.toString().toIntOrNull() ?: 0,
            coin1 = coinEditTexts[1]?.text.toString().toIntOrNull() ?: 0
        )

        lifecycleScope.launch {
            try {
                ApiClient.cashbookApi.saveCashIn(request)
                Toast.makeText(this@AdminCashBookActivity, "Cash Inventory synced successfully!", Toast.LENGTH_SHORT).show()
                loadDashboardData()
            } catch (e: Exception) {
                Toast.makeText(this@AdminCashBookActivity, "Sync failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
