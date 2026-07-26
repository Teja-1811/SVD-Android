package com.svd.svdagencies.ui.admin.cashbook

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.Cashbook.CashbookDashboardResponse
import com.svd.svdagencies.data.model.admin.Cashbook.DeliverySalaryAgent
import com.svd.svdagencies.data.model.admin.Cashbook.DeliverySalaryPaymentRequest
import com.svd.svdagencies.data.model.admin.Cashbook.SaveCashInRequest
import com.svd.svdagencies.databinding.AdminCashbookBinding
import com.svd.svdagencies.databinding.AdminStatCardBinding
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import com.svd.svdagencies.utils.NetworkMessageUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AdminCashBookActivity : AdminBaseActivity() {

    private lateinit var binding: AdminCashbookBinding
    private lateinit var companyDueAdapter: CompanyDueAdapter

    private val noteValues = listOf(500, 200, 100, 50, 20, 10)
    private val noteEditTexts = mutableMapOf<Int, EditText>()
    private val noteTotalViews = mutableMapOf<Int, TextView>()

    private val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    private var selectedMonth: Int? = null
    private var selectedYear: Int? = null
    private var salaryAgents: List<DeliverySalaryAgent> = emptyList()

    private val expenseLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            loadDashboardData()
        }
    }

    private val expenseHistoryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        loadDashboardData()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdminCashbookBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAdminLayout("CashBook")
        initViews()
        setupFilters()
        setupCashInGrids()
        loadDashboardData()
    }

    private fun initViews() {
        // Initialize Stat Cards
        setupStatCard(AdminStatCardBinding.bind(binding.statCash.root), "CASH IN HAND", R.drawable.ic_money, "#E3F2FD", "#1976D2")
        setupStatCard(AdminStatCardBinding.bind(binding.statBank.root), "BANK BALANCE", R.drawable.ic_payments, "#E0F2F1", "#00796B")
        setupStatCard(AdminStatCardBinding.bind(binding.statDues.root), "CUSTOMER DUES", R.drawable.ic_person, "#FCE4EC", "#C2185B")
        setupStatCard(AdminStatCardBinding.bind(binding.statNetCash.root), "NET CASH", R.drawable.ic_money_bag, "#E8F5E9", "#388E3C")
        setupStatCard(AdminStatCardBinding.bind(binding.statExpenses.root), "MONTHLY EXPENSES", R.drawable.ic_bill, "#FFF3E0", "#F57C00")
        setupStatCard(AdminStatCardBinding.bind(binding.statProfit.root), "MONTHLY PROFIT", R.drawable.ic_swap, "#F3E5F5", "#7B1FA2")
        setupStatCard(AdminStatCardBinding.bind(binding.statStockValue.root), "STOCK VALUE", R.drawable.ic_stock, "#EFEBE9", "#5D4037")
        setupStatCard(AdminStatCardBinding.bind(binding.statRAmount.root), "REMAINING AMOUNT", R.drawable.ic_rupee, "#F1F8E9", "#689F38")
        setupStatCard(AdminStatCardBinding.bind(binding.statNetProfit.root), "NET PROFIT", R.drawable.ic_cashbook, "#E0F7FA", "#0097A7")

        companyDueAdapter = CompanyDueAdapter(emptyList())
        binding.rvCompanyDues.apply {
            layoutManager = LinearLayoutManager(this@AdminCashBookActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = companyDueAdapter
            
            // Add SnapHelper for slider behavior
            val snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(this)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val position = layoutManager.findFirstCompletelyVisibleItemPosition()
                    if (position != RecyclerView.NO_POSITION) {
                        updateIndicators(position)
                    }
                }
            })
        }

        binding.btnUpdateCashIn.setOnClickListener { updateCashIn() }
        
        binding.btnAddExpense.setOnClickListener {
            expenseLauncher.launch(Intent(this, AddExpenseActivity::class.java))
        }
        
        binding.btnViewExpenses.setOnClickListener {
            expenseHistoryLauncher.launch(Intent(this, ViewExpensesActivity::class.java))
        }

        binding.btnPayDeliverySalary.setOnClickListener {
            showDeliverySalaryDialog()
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadDashboardData()
        }

        binding.btnApplyFilters.setOnClickListener {
            loadDashboardData()
        }

        binding.btnResetFilters.setOnClickListener {
            resetFilters()
        }
    }

    private fun setupIndicators(count: Int) {
        binding.layoutDuesIndicators.removeAllViews()
        if (count <= 1) return

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(8, 0, 8, 0)

        for (i in 0 until count) {
            val dot = ImageView(this)
            dot.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.bg_red_dot))
            dot.alpha = 0.3f
            dot.layoutParams = params
            binding.layoutDuesIndicators.addView(dot)
        }
        updateIndicators(0)
    }

    private fun updateIndicators(position: Int) {
        for (i in 0 until binding.layoutDuesIndicators.childCount) {
            val view = binding.layoutDuesIndicators.getChildAt(i) as ImageView
            if (i == position) {
                view.alpha = 1.0f
                view.scaleX = 1.2f
                view.scaleY = 1.2f
            } else {
                view.alpha = 0.3f
                view.scaleX = 1.0f
                view.scaleY = 1.0f
            }
        }
    }

    private fun setupFilters() {
        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, months)
        binding.spinnerMonth.setAdapter(monthAdapter)

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear - 2..currentYear + 1).map { it.toString() }
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, years)
        binding.spinnerYear.setAdapter(yearAdapter)

        val currentMonthIdx = Calendar.getInstance().get(Calendar.MONTH)
        binding.spinnerMonth.setText(months[currentMonthIdx], false)
        binding.spinnerYear.setText(currentYear.toString(), false)
        
        selectedMonth = currentMonthIdx + 1
        selectedYear = currentYear

        binding.spinnerMonth.setOnItemClickListener { _, _, position, _ ->
            selectedMonth = position + 1
        }

        binding.spinnerYear.setOnItemClickListener { _, _, position, _ ->
            selectedYear = years[position].toInt()
        }
    }

    private fun resetFilters() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val currentMonthIdx = Calendar.getInstance().get(Calendar.MONTH)
        
        binding.spinnerMonth.setText(months[currentMonthIdx], false)
        binding.spinnerYear.setText(currentYear.toString(), false)
        
        selectedMonth = currentMonthIdx + 1
        selectedYear = currentYear
        
        loadDashboardData()
    }

    private fun setupStatCard(cardBinding: AdminStatCardBinding, label: String, iconRes: Int, bgColor: String, iconTint: String) {
        cardBinding.tvStatLabel.text = label
        cardBinding.ivStatIcon.setImageResource(iconRes)
        cardBinding.cardIcon.setCardBackgroundColor(Color.parseColor(bgColor))
        cardBinding.ivStatIcon.setColorFilter(Color.parseColor(iconTint))
    }

    private fun setupCashInGrids() {
        binding.gridNotes.removeAllViews()
        noteEditTexts.clear()
        noteTotalViews.clear()

        for (value in noteValues) {
            val view = layoutInflater.inflate(R.layout.admin_cash_denomination, binding.gridNotes, false)
            val etCount = view.findViewById<EditText>(R.id.etDenomCount)
            val tvTotal = view.findViewById<TextView>(R.id.tvDenomTotal)
            val tvLabel = view.findViewById<TextView>(R.id.tvDenomLabel)

            tvLabel.text = "₹$value"
            noteEditTexts[value] = etCount
            noteTotalViews[value] = tvTotal

            etCount.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val countInt = s.toString().toIntOrNull() ?: 0
                    tvTotal.text = "₹${countInt * value}"
                    updateLiveCashTotal() 
                }
            })
            binding.gridNotes.addView(view)
        }
    }

    private fun updateLiveCashTotal() {
        var total = 0.0
        noteEditTexts.forEach { (value, et) ->
            total += (et.text.toString().toIntOrNull() ?: 0) * value
        }
        binding.statCash.tvStatValue.text = "₹%.2f".format(total)
    }

    private fun loadDashboardData() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val response = ApiClient.cashbookApi.getDashboardData(selectedMonth, selectedYear)
                populateUI(response)
            } catch (e: Exception) {
                if (!isFinishing) {
                    Toast.makeText(
                        this@AdminCashBookActivity,
                        NetworkMessageUtils.friendlyMessage(e, "Failed to load cashbook data"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                binding.swipeRefresh.isRefreshing = false
                hideScreenLoading()
            }
        }
    }

    private fun populateUI(data: CashbookDashboardResponse) {
        // Stats
        binding.statCash.tvStatValue.text = "₹%.2f".format(data.cash_in)
        binding.statBank.tvStatValue.text = "₹%.2f".format(data.bank_balance)
        binding.statDues.tvStatValue.text = "₹%.2f".format(data.total_customer_dues)
        binding.statNetCash.tvStatValue.text = "₹%.2f".format(data.net_cash)
        binding.statExpenses.tvStatValue.text = "₹%.2f".format(data.cash_out)
        binding.statProfit.tvStatValue.text = "₹%.2f".format(data.monthly_profit)
        binding.statStockValue.tvStatValue.text = "₹%.2f".format(data.stock_value)
        binding.statRAmount.tvStatValue.text = "₹%.2f".format(data.remaining_amount)
        binding.statNetProfit.tvStatValue.text = "₹%.2f".format(data.net_profit)

        val salarySummary = data.delivery_salary
        salaryAgents = salarySummary?.agents.orEmpty()
        binding.tvDeliverySalaryDue.text = "Delivery salary due: ₹%.2f".format(salarySummary?.remaining_salary ?: 0.0)

        companyDueAdapter.update(data.company_dues)
        setupIndicators(data.company_dues.size)
        binding.tvTotalCompanyDues.text = "Total Company Dues: ₹%.2f".format(data.total_company_dues)

        // Denomination Loading
        data.denominations.let { d ->
            setDenominationValue(500, d.c500)
            setDenominationValue(200, d.c200)
            setDenominationValue(100, d.c100)
            setDenominationValue(50, d.c50)
            setDenominationValue(20, d.c20)
            setDenominationValue(10, d.c10)
        }
    }

    private fun setDenominationValue(note: Int, count: Int) {
        val et = noteEditTexts[note] ?: return
        val tv = noteTotalViews[note] ?: return
        
        val newText = if (count == 0) "" else count.toString()
        if (et.text.toString() != newText) {
            et.setText(newText)
        }
        tv.text = "₹${count * note}"
    }

    private fun updateCashIn() {
        val request = SaveCashInRequest(
            c500 = noteEditTexts[500]?.text.toString().toIntOrNull() ?: 0,
            c200 = noteEditTexts[200]?.text.toString().toIntOrNull() ?: 0,
            c100 = noteEditTexts[100]?.text.toString().toIntOrNull() ?: 0,
            c50 = noteEditTexts[50]?.text.toString().toIntOrNull() ?: 0,
            c20 = noteEditTexts[20]?.text.toString().toIntOrNull() ?: 0,
            c10 = noteEditTexts[10]?.text.toString().toIntOrNull() ?: 0,
            coin20 = 0,
            coin10 = 0,
            coin5 = 0,
            coin2 = 0,
            coin1 = 0
        )

        showScreenLoading()
        lifecycleScope.launch {
            try {
                ApiClient.cashbookApi.saveCashIn(request)
                Toast.makeText(this@AdminCashBookActivity, "Cash Inventory synced successfully!", Toast.LENGTH_SHORT).show()
                loadDashboardData()
            } catch (e: Exception) {
                hideScreenLoading()
                Toast.makeText(
                    this@AdminCashBookActivity,
                    NetworkMessageUtils.friendlyMessage(e, "Failed to sync cash inventory"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showDeliverySalaryDialog() {
        if (salaryAgents.isEmpty()) {
            Toast.makeText(this, "No delivery agents found", Toast.LENGTH_SHORT).show()
            return
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 12, 32, 0)
        }
        val agentInput = android.widget.AutoCompleteTextView(this).apply {
            hint = "Select delivery customer / agent"
            setAdapter(
                ArrayAdapter(
                    this@AdminCashBookActivity,
                    android.R.layout.simple_dropdown_item_1line,
                    salaryAgents.map { "${it.agent_name ?: "Agent"} - Due ₹%.2f".format(it.remaining_salary) }
                )
            )
            setThreshold(0)
            inputType = android.text.InputType.TYPE_NULL
            setOnClickListener { showDropDown() }
            setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDropDown() }
        }
        val amountInput = EditText(this).apply {
            hint = "Amount paid"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val notesInput = EditText(this).apply { hint = "Notes" }

        container.addView(agentInput)
        container.addView(amountInput)
        container.addView(notesInput)

        var selectedAgent: DeliverySalaryAgent? = null
        agentInput.setOnItemClickListener { _, _, position, _ ->
            selectedAgent = salaryAgents.getOrNull(position)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Record Delivery Salary")
            .setView(container)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val agent = selectedAgent
                val amount = amountInput.text.toString().toDoubleOrNull()
                if (agent == null || amount == null || amount <= 0) {
                    Toast.makeText(this, "Select agent and valid amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                saveDeliverySalary(agent.agent_id, amount, notesInput.text.toString())
            }
            .show()
    }

    private fun saveDeliverySalary(agentId: Int, amount: Double, notes: String) {
        showScreenLoading()
        lifecycleScope.launch {
            try {
                ApiClient.cashbookApi.addDeliveryAgentSalary(
                    DeliverySalaryPaymentRequest(
                        delivery_agent_id = agentId,
                        amount = amount,
                        payment_date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date()),
                        notes = notes
                    )
                )
                Toast.makeText(this@AdminCashBookActivity, "Salary recorded", Toast.LENGTH_SHORT).show()
                loadDashboardData()
            } catch (e: Exception) {
                hideScreenLoading()
                Toast.makeText(
                    this@AdminCashBookActivity,
                    NetworkMessageUtils.friendlyMessage(e, "Failed to record salary"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
