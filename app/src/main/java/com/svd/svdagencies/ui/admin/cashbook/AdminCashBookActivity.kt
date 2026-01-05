package com.svd.svdagencies.ui.admin.cashbook

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.svd.svdagencies.R
import com.svd.svdagencies.ui.admin.AdminBaseActivity

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

    // Denominations
    private val notes = listOf(500, 200, 100, 50, 20, 10)
    private val coins = listOf(20, 10, 5, 2, 1)
    
    // Map to store counts: denomination -> count
    private val denominationCounts = mutableMapOf<Int, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_cashbook)

        setupAdminLayout("CashBook")

        initViews()
        setupCashInGrids()
        loadMockData()
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
        
        btnUpdateBank.setOnClickListener {
            Toast.makeText(this, "Bank Balance Updated", Toast.LENGTH_SHORT).show()
        }

        gridNotes = findViewById(R.id.gridNotes)
        gridCoins = findViewById(R.id.gridCoins)
        btnUpdateCashIn = findViewById(R.id.btnUpdateCashIn)

        btnUpdateCashIn.setOnClickListener {
            calculateTotalCashIn()
            Toast.makeText(this, "Cash In Updated", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<MaterialButton>(R.id.btnAddExpense).setOnClickListener {
            Toast.makeText(this, "Add Expense Clicked", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<MaterialButton>(R.id.btnViewExpenses).setOnClickListener {
             Toast.makeText(this, "View Expenses Clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCashInGrids() {
        // Clear grids
        gridNotes.removeAllViews()
        gridCoins.removeAllViews()

        // Populate Notes
        for (noteValue in notes) {
            val view = LayoutInflater.from(this).inflate(R.layout.item_denomination_input, gridNotes, false)
            setupDenominationItem(view, noteValue)
            
            // Set layout params for GridLayout
            val params = GridLayout.LayoutParams()
            params.width = 0
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            view.layoutParams = params
            
            gridNotes.addView(view)
        }

        // Populate Coins
        for (coinValue in coins) {
            val view = LayoutInflater.from(this).inflate(R.layout.item_denomination_input, gridCoins, false)
            setupDenominationItem(view, coinValue)
            
             // Set layout params for GridLayout
            val params = GridLayout.LayoutParams()
            params.width = 0
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            view.layoutParams = params
            
            gridCoins.addView(view)
        }
    }

    private fun setupDenominationItem(view: android.view.View, value: Int) {
        val tvLabel = view.findViewById<TextView>(R.id.tvLabel)
        val etCount = view.findViewById<EditText>(R.id.etCount)
        val tvTotal = view.findViewById<TextView>(R.id.tvTotal)

        tvLabel.text = "₹$value"
        etCount.hint = "0"
        
        // Initialize count
        denominationCounts[value] = 0

        etCount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val countInt = s.toString().toIntOrNull() ?: 0
                denominationCounts[value] = countInt
                val total = countInt * value
                tvTotal.text = "Total: ₹$total"
            }
        })
    }
    
    private fun calculateTotalCashIn() {
        var total = 0
        for ((value, count) in denominationCounts) {
            total += value * count
        }
        // Ideally send this to server or update UI
        // For now just log or toast
        // Toast.makeText(this, "Total Cash In: ₹$total", Toast.LENGTH_SHORT).show()
    }

    private fun loadMockData() {
        tvCash.text = "₹7400.00"
        tvBank.text = "₹257.22"
        tvDues.text = "₹2802.00"
        tvNetCash.text = "₹10459.22"
        tvExpenses.text = "₹1846.46"
        tvProfit.text = "₹3939.51"
        tvStockValue.text = "₹5515.06"
        tvRAmount.text = "₹310.65"
        tvNetProfit.text = "₹2093.05"
        
        etBankBalance.setText("257.22")

        // Mock Company Dues
        addCompanyDueCard("Dodla", "₹12053.58", "Last updated: 31 Jan 2026")
        addCompanyDueCard("Vallabha", "₹1517.00", "Last updated: 31 Jan 2026")
        
        tvTotalCompanyDues.text = "Total Dues: ₹13570.58"
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
        tvName.setTextColor(android.graphics.Color.parseColor("#FF9800")) // Orange
        
        val tvAmount = TextView(this)
        tvAmount.text = amount
        tvAmount.textSize = 20f
        tvAmount.setTypeface(null, android.graphics.Typeface.BOLD)
        tvAmount.setTextColor(android.graphics.Color.parseColor("#C62828")) // Red
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
