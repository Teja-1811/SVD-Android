package com.svd.svdagencies.ui.admin.cashbook

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.CashbookDashboardResponse
import com.svd.svdagencies.data.model.admin.SaveBankBalanceRequest
import com.svd.svdagencies.data.model.admin.SaveCashInRequest
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import kotlinx.coroutines.launch

class AdminCashBookActivity : AdminBaseActivity() {

    // Stats
    private lateinit var tvCash: TextView
    private lateinit var tvBank: TextView
    private lateinit var tvDues: TextView
    private lateinit var tvNetCash: TextView
    private lateinit var tvExpenses: TextView
    private lateinit var tvProfit: TextView
    private lateinit var tvStockValue: TextView
    private lateinit var tvRAmount: TextView
    private lateinit var tvNetProfit: TextView

    // Company Dues
    private lateinit var layoutCompanyDues: android.widget.LinearLayout
    private lateinit var tvTotalCompanyDues: TextView

    // Bank Balance
    private lateinit var etBankBalance: EditText
    private lateinit var btnUpdateBank: MaterialButton

    // Cash In
    private lateinit var gridNotes: GridLayout
    private lateinit var gridCoins: GridLayout
    private lateinit var btnUpdateCashIn: MaterialButton

    // Denominations structure to match API
    private val notes = listOf(500, 200, 100, 50, 20, 10)
    private val coins = listOf(20, 10, 5, 2, 1)
    
    // Separate maps for notes and coins to handle collisions (like 20 and 10)
    private val noteEditTexts = mutableMapOf<Int, EditText>()
    private val coinEditTexts = mutableMapOf<Int, EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_cashbook)

        setupAdminLayout("CashBook")

        initViews()
        setupCashInGrids()
        loadDashboardData() 
    }

    private fun initViews() {
        tvCash = findViewById(R.id.tvCash)
        tvBank = findViewById(R.id.tvBank)
        tvDues = findViewById(R.id.tvDues)
        tvNetCash = findViewById(R.id.tvNetCash)
        tvExpenses = findViewById(R.id.tvExpenses)
        tvProfit = findViewById(R.id.tvProfit)
        tvStockValue = findViewById(R.id.tvStockValue)
        tvRAmount = findViewById(R.id.tvRAmount)
        tvNetProfit = findViewById(R.id.tvNetProfit)

        layoutCompanyDues = findViewById(R.id.layoutCompanyDues)
        tvTotalCompanyDues = findViewById(R.id.tvTotalCompanyDues)

        etBankBalance = findViewById(R.id.etBankBalance)
        btnUpdateBank = findViewById(R.id.btnUpdateBank)
        
        btnUpdateBank.setOnClickListener { updateBankBalance() }

        gridNotes = findViewById(R.id.gridNotes)
        gridCoins = findViewById(R.id.gridCoins)
        btnUpdateCashIn = findViewById(R.id.btnUpdateCashIn)

        btnUpdateCashIn.setOnClickListener { updateCashIn() }
        
        findViewById<MaterialButton>(R.id.btnAddExpense).setOnClickListener {
            Toast.makeText(this, "Add Expense Clicked", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<MaterialButton>(R.id.btnViewExpenses).setOnClickListener {
             Toast.makeText(this, "View Expenses Clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCashInGrids() {
        gridNotes.removeAllViews()
        gridCoins.removeAllViews()
        noteEditTexts.clear()
        coinEditTexts.clear()

        for (value in notes) {
            val view = LayoutInflater.from(this).inflate(R.layout.admin_cashbook_denomination_input, gridNotes, false)
            val et = setupDenominationItem(view, value, isCoin = false)
            noteEditTexts[value] = et
            
            val params = GridLayout.LayoutParams()
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            params.width = 0 
            view.layoutParams = params
            gridNotes.addView(view)
        }

        for (value in coins) {
            val view = LayoutInflater.from(this).inflate(R.layout.admin_cashbook_denomination_input, gridCoins, false)
            val et = setupDenominationItem(view, value, isCoin = true)
            coinEditTexts[value] = et
            
            val params = GridLayout.LayoutParams()
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            params.width = 0
            view.layoutParams = params
            gridCoins.addView(view)
        }
    }

    private fun setupDenominationItem(view: android.view.View, value: Int, isCoin: Boolean): EditText {
        val tvLabel = view.findViewById<TextView>(R.id.tvLabel)
        val etCount = view.findViewById<EditText>(R.id.etCount)
        val tvTotal = view.findViewById<TextView>(R.id.tvTotal)

        tvLabel.text = "₹$value"
        etCount.hint = "0"
        
        etCount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val countInt = s.toString().toIntOrNull() ?: 0
                tvTotal.text = "Total: ₹${countInt * value}"
            }
        })
        return etCount
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.cashbookApi.getDashboardData()
                populateUI(response)
            } catch (e: Exception) {
                Toast.makeText(this@AdminCashBookActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun populateUI(data: CashbookDashboardResponse) {
        tvCash.text = "₹%.2f".format(data.cash_in)
        tvBank.text = "₹%.2f".format(data.bank_balance)
        tvDues.text = "₹%.2f".format(data.total_customer_dues)
        tvNetCash.text = "₹%.2f".format(data.net_cash)
        tvExpenses.text = "₹%.2f".format(data.cash_out)
        tvProfit.text = "₹%.2f".format(data.monthly_profit)
        tvStockValue.text = "₹%.2f".format(data.stock_value)
        tvRAmount.text = "₹%.2f".format(data.remaining_amount)
        tvNetProfit.text = "₹%.2f".format(data.net_profit)

        etBankBalance.setText(data.bank_balance.toString())

        layoutCompanyDues.removeAllViews()
        data.company_dues.forEach { due ->
            addCompanyDueCard(due.company_name, "₹%.2f".format(due.total_due), "Last updated: ${due.last_updated}")
        }
        tvTotalCompanyDues.text = "Total Dues: ₹%.2f".format(data.total_company_dues)
        
        data.denominations?.let { d ->
            noteEditTexts[500]?.setText(d.c500.toString())
            noteEditTexts[200]?.setText(d.c200.toString())
            noteEditTexts[100]?.setText(d.c100.toString())
            noteEditTexts[50]?.setText(d.c50.toString())
            noteEditTexts[20]?.setText(d.c20.toString())
            noteEditTexts[10]?.setText(d.c10.toString())
            
            coinEditTexts[20]?.setText(d.coin20.toString())
            coinEditTexts[10]?.setText(d.coin10.toString())
            coinEditTexts[5]?.setText(d.coin5.toString())
            coinEditTexts[2]?.setText(d.coin2.toString())
            coinEditTexts[1]?.setText(d.coin1.toString())
        }
    }

    private fun updateBankBalance() {
        val balance = etBankBalance.text.toString().toDoubleOrNull() ?: return
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
                Toast.makeText(this@AdminCashBookActivity, "Cash in updated", Toast.LENGTH_SHORT).show()
                loadDashboardData()
            } catch (e: Exception) {
                Toast.makeText(this@AdminCashBookActivity, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addCompanyDueCard(name: String, amount: String, date: String) {
        val card = MaterialCardView(this)
        val params = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 8, 0, 8)
        card.layoutParams = params
        card.radius = 16f
        card.cardElevation = 4f
        card.setContentPadding(24, 24, 24, 24)
        card.setCardBackgroundColor(android.graphics.Color.WHITE)
        card.strokeWidth = 2
        card.strokeColor = android.graphics.Color.parseColor("#DDDDDD")

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        
        val tvName = TextView(this)
        tvName.text = name
        tvName.textSize = 16f
        tvName.setTypeface(null, android.graphics.Typeface.BOLD)
        tvName.setTextColor(android.graphics.Color.parseColor("#FF9800"))
        
        val tvAmount = TextView(this)
        tvAmount.text = amount
        tvAmount.textSize = 20f
        tvAmount.setTypeface(null, android.graphics.Typeface.BOLD)
        tvAmount.setTextColor(android.graphics.Color.parseColor("#C62828"))
        tvAmount.setPadding(0, 8, 0, 8)

        val tvDate = TextView(this)
        tvDate.text = date
        tvDate.textSize = 12f
        tvDate.setTextColor(android.graphics.Color.GRAY)

        layout.addView(tvName)
        layout.addView(tvAmount)
        layout.addView(tvDate)
        
        card.addView(layout)
        layoutCompanyDues.addView(card)
    }
}
