package com.svd.svdagencies.ui.admin.cashbook

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageButton
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
import com.google.android.material.textfield.TextInputEditText
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.Cashbook.CashbookDashboardResponse
import com.svd.svdagencies.data.model.admin.Cashbook.CommissionCredit
import com.svd.svdagencies.data.model.admin.Cashbook.CompanyOption
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
import java.util.Date
import java.util.Locale

class AdminCashBookActivity : AdminBaseActivity() {

    private lateinit var binding: AdminCashbookBinding
    private lateinit var companyDueAdapter: CompanyDueAdapter
    private lateinit var commissionAdapter: CommissionCreditAdapter

    private val noteValues = listOf(500, 200, 100, 50, 20, 10)
    private val coinValues = listOf(20, 10, 5, 2, 1)
    
    private val cashEditTexts = mutableMapOf<Int, EditText>()
    private val cashTotalViews = mutableMapOf<Int, TextView>()
    private val coinEditTexts = mutableMapOf<Int, EditText>()
    private val coinTotalViews = mutableMapOf<Int, TextView>()

    private val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    private var selectedMonth: Int? = null
    private var selectedYear: Int? = null
    private var salaryAgents: List<DeliverySalaryAgent> = emptyList()
    private var allCompanies: List<CompanyOption> = emptyList()
    private var commissionCredits: List<CommissionCredit> = emptyList()

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
    }

    override fun onResume() {
        super.onResume()
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
        setupStatCard(AdminStatCardBinding.bind(binding.statLoss.root), "LEAKAGE LOSS", R.drawable.ic_warning, "#FFEBEE", "#D32F2F")
        setupStatCard(AdminStatCardBinding.bind(binding.statSalary.root), "SALARY PAID", R.drawable.ic_person, "#E8F5E9", "#2E7D32")
        setupStatCard(AdminStatCardBinding.bind(binding.statCommission.root), "COMMISSION", R.drawable.ic_money, "#FFFDE7", "#FBC02D")

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

        commissionAdapter = CommissionCreditAdapter(
            emptyList(),
            onEdit = { showAddCommissionDialog(it) },
            onDelete = { showDeleteCommissionConfirm(it) }
        )
        binding.rvCommissionCredits.apply {
            layoutManager = LinearLayoutManager(this@AdminCashBookActivity)
            adapter = commissionAdapter
        }

        binding.btnUpdateCashIn.setOnClickListener { updateCashIn() }
        
        binding.btnAddExpense.setOnClickListener {
            expenseLauncher.launch(Intent(this, AddExpenseActivity::class.java))
        }

        binding.btnPayDeliverySalary.setOnClickListener {
            showDeliverySalaryDialog()
        }

        binding.btnCommission.setOnClickListener {
            showAddCommissionDialog()
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
        cashEditTexts.clear()
        cashTotalViews.clear()
        coinEditTexts.clear()
        coinTotalViews.clear()

        for (value in noteValues) {
            addCashItem(value, true)
        }
        for (value in coinValues) {
            addCashItem(value, false)
        }
    }

    private fun addCashItem(value: Int, isNote: Boolean) {
        val view = layoutInflater.inflate(R.layout.admin_cash_denomination, binding.gridNotes, false)
        val etCount = view.findViewById<EditText>(R.id.etDenomCount)
        val tvTotal = view.findViewById<TextView>(R.id.tvDenomTotal)
        val tvLabel = view.findViewById<TextView>(R.id.tvDenomLabel)

        tvLabel.text = if (isNote) "₹$value" else "₹$value C"
        
        if (isNote) {
            cashEditTexts[value] = etCount
            cashTotalViews[value] = tvTotal
        } else {
            coinEditTexts[value] = etCount
            coinTotalViews[value] = tvTotal
        }

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

    private fun updateLiveCashTotal() {
        var total = 0.0
        cashEditTexts.forEach { (value, et) ->
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
        val s = data.summary
        binding.statCash.tvStatValue.text = "₹%.2f".format(s.cash_in)
        binding.statBank.tvStatValue.text = "₹%.2f".format(s.bank_balance)
        binding.statDues.tvStatValue.text = "₹%.2f".format(s.customer_due)
        binding.statNetCash.tvStatValue.text = "₹%.2f".format(s.net_cash)
        binding.statExpenses.tvStatValue.text = "₹%.2f".format(s.cash_out)
        binding.statProfit.tvStatValue.text = "₹%.2f".format(s.monthly_profit)
        binding.statStockValue.tvStatValue.text = "₹%.2f".format(s.stock_value)
        binding.statRAmount.tvStatValue.text = "₹%.2f".format(s.remaining_amount)
        binding.statNetProfit.tvStatValue.text = "₹%.2f".format(s.net_profit)
        binding.statLoss.tvStatValue.text = "₹%.2f".format(s.monthly_loss)
        binding.statSalary.tvStatValue.text = "₹%.2f".format(s.salary_paid)
        binding.statCommission.tvStatValue.text = "₹%.2f".format(s.commission_credit)

        val salarySummary = data.deliverySalary
        salaryAgents = salarySummary?.agents.orEmpty()
        binding.tvDeliverySalaryDue.text = getString(R.string.label_delivery_salary_due, salarySummary?.remaining_salary ?: 0.0)

        companyDueAdapter.update(data.companyDues)
        setupIndicators(data.companyDues.size)
        binding.tvTotalCompanyDues.text = getString(R.string.label_total_company_dues, s.company_due)

        allCompanies = data.filters.companies
        commissionCredits = data.commissionCredits
        
        commissionAdapter.update(commissionCredits)
        binding.tvNoCommission.visibility = if (commissionCredits.isEmpty()) View.VISIBLE else View.GONE

        // Denomination Loading
        data.cashEntry.denominations.forEach { item ->
            val value = item.name.replace("₹", "").replace(" Coin", "").trim().toIntOrNull() ?: 0
            val isNote = !item.name.contains("Coin")
            setDenominationValue(value, item.count, isNote)
        }
    }

    private fun setDenominationValue(value: Int, count: Int, isNote: Boolean) {
        val et = if (isNote) cashEditTexts[value] else coinEditTexts[value]
        val tv = if (isNote) cashTotalViews[value] else coinTotalViews[value]
        
        if (et == null || tv == null) return
        
        val newText = if (count == 0) "" else count.toString()
        if (et.text.toString() != newText) {
            et.setText(newText)
        }
        tv.text = "₹${count * value}"
    }

    private fun updateCashIn() {
        val request = SaveCashInRequest(
            c500 = cashEditTexts[500]?.text.toString().toIntOrNull() ?: 0,
            c200 = cashEditTexts[200]?.text.toString().toIntOrNull() ?: 0,
            c100 = cashEditTexts[100]?.text.toString().toIntOrNull() ?: 0,
            c50 = cashEditTexts[50]?.text.toString().toIntOrNull() ?: 0,
            c20 = cashEditTexts[20]?.text.toString().toIntOrNull() ?: 0,
            c10 = cashEditTexts[10]?.text.toString().toIntOrNull() ?: 0,
            coin20 = coinEditTexts[20]?.text.toString().toIntOrNull() ?: 0,
            coin10 = coinEditTexts[10]?.text.toString().toIntOrNull() ?: 0,
            coin5 = coinEditTexts[5]?.text.toString().toIntOrNull() ?: 0,
            coin2 = coinEditTexts[2]?.text.toString().toIntOrNull() ?: 0,
            coin1 = coinEditTexts[1]?.text.toString().toIntOrNull() ?: 0
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
        val agentInput = AutoCompleteTextView(this).apply {
            hint = "Select delivery customer / agent"
            setAdapter(
                ArrayAdapter(
                    this@AdminCashBookActivity,
                    android.R.layout.simple_dropdown_item_1line,
                    salaryAgents.map { "${it.agent_name ?: "Agent"} - Due ₹%.2f".format(it.remaining_salary) }
                )
            )
            setThreshold(0)
            inputType = InputType.TYPE_NULL
            setOnClickListener { showDropDown() }
            setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDropDown() }
        }
        val amountInput = EditText(this).apply {
            hint = "Amount paid"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
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
                        payment_date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
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

    private fun showAddCommissionDialog(existing: CommissionCredit? = null) {
        val view = layoutInflater.inflate(R.layout.admin_dialog_commission, null)
        val actvCompany = view.findViewById<AutoCompleteTextView>(R.id.actvCompany)
        val etAmount = view.findViewById<TextInputEditText>(R.id.etAmount)
        val etDescription = view.findViewById<TextInputEditText>(R.id.etDescription)

        val companyNames = allCompanies.map { it.name }
        val companyAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, companyNames)
        actvCompany.setAdapter(companyAdapter)

        existing?.let {
            actvCompany.setText(it.company, false)
            etAmount.setText(it.amount.toString())
            etDescription.setText(it.description)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(if (existing == null) "Add Commission Credit" else "Edit Commission Credit")
            .setView(view)
            .setPositiveButton(if (existing == null) "Save" else "Update") { _, _ ->
                val selectedName = actvCompany.text.toString()
                val companyId = allCompanies.find { it.name == selectedName }?.id
                val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
                val desc = etDescription.text.toString()

                if (companyId != null && amount > 0) {
                    if (existing == null) {
                        saveCommissionCredit(companyId, amount, desc)
                    } else {
                        updateCommissionCredit(existing.id, companyId, amount, desc)
                    }
                } else {
                    Toast.makeText(this, "Valid Company and Amount required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveCommissionCredit(companyId: Int, amount: Double, description: String) {
        showScreenLoading()
        lifecycleScope.launch {
            try {
                val request = mapOf(
                    "company" to companyId,
                    "amount" to amount,
                    "description" to description
                )
                ApiClient.cashbookApi.addCommissionCredit(request)
                Toast.makeText(this@AdminCashBookActivity, "Commission added", Toast.LENGTH_SHORT).show()
                loadDashboardData()
            } catch (e: Exception) {
                hideScreenLoading()
                Toast.makeText(this@AdminCashBookActivity, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateCommissionCredit(id: Int, companyId: Int, amount: Double, description: String) {
        showScreenLoading()
        lifecycleScope.launch {
            try {
                val request = mapOf(
                    "company" to companyId,
                    "amount" to amount,
                    "description" to description
                )
                ApiClient.cashbookApi.editCommissionCredit(id, request)
                Toast.makeText(this@AdminCashBookActivity, "Commission updated", Toast.LENGTH_SHORT).show()
                loadDashboardData()
            } catch (e: Exception) {
                hideScreenLoading()
                Toast.makeText(this@AdminCashBookActivity, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteCommissionConfirm(credit: CommissionCredit) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Commission")
            .setMessage("Are you sure you want to delete this commission credit of ₹${credit.amount} from ${credit.company}?")
            .setPositiveButton("Delete") { _, _ -> deleteCommissionCredit(credit.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCommissionCredit(id: Int) {
        showScreenLoading()
        lifecycleScope.launch {
            try {
                ApiClient.cashbookApi.deleteCommissionCredit(id)
                Toast.makeText(this@AdminCashBookActivity, "Commission deleted", Toast.LENGTH_SHORT).show()
                loadDashboardData()
            } catch (e: Exception) {
                hideScreenLoading()
                Toast.makeText(this@AdminCashBookActivity, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class CommissionCreditAdapter(
    private var credits: List<CommissionCredit>,
    private val onEdit: (CommissionCredit) -> Unit,
    private val onDelete: (CommissionCredit) -> Unit
) : RecyclerView.Adapter<CommissionCreditAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCompany: TextView = view.findViewById(R.id.tvCompanyName)
        val tvDesc: TextView = view.findViewById(R.id.tvDescription)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    fun update(newCredits: List<CommissionCredit>) {
        credits = newCredits
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.admin_commission_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val credit = credits[position]
        holder.tvCompany.text = credit.company
        holder.tvDesc.text = credit.description
        holder.tvAmount.text = "₹%.2f".format(credit.amount)
        holder.tvDate.text = "Added: ${credit.createdAt.take(16).replace("T", " ")}"
        
        holder.btnEdit.setOnClickListener { onEdit(credit) }
        holder.btnDelete.setOnClickListener { onDelete(credit) }
    }

    override fun getItemCount() = credits.size
}
