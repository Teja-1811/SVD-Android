package com.svd.svdagencies.ui.admin.cashbook

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.LinearLayout
import android.view.Gravity
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import java.text.NumberFormat
import java.util.Locale
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.Cashbook.StatementResponse
import com.svd.svdagencies.data.model.admin.Cashbook.StatementSummaryRow
import com.svd.svdagencies.data.model.admin.Cashbook.StatementTransaction
import com.svd.svdagencies.databinding.AdminStatementBinding
import com.svd.svdagencies.databinding.AdminStatementStatBinding
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import com.svd.svdagencies.utils.NetworkMessageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class AdminStatementActivity : AdminBaseActivity() {
    private var filterRequest: Job? = null


    private lateinit var binding: AdminStatementBinding
    private var selectedMonth: Int? = null
    private var selectedYear: Int? = null

    private val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdminStatementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupAdminLayout("Statement")
        setupStats()
        setupFilters()
        binding.btnApplyFilters.icon = null
        binding.btnApplyFilters.layoutParams.width = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        binding.btnApplyFilters.minimumWidth = (72 * resources.displayMetrics.density).toInt()
        com.svd.svdagencies.utils.RefreshButtonStyle.apply(binding.btnApplyFilters)
        binding.btnApplyFilters.contentDescription = "Clear filters"
        binding.btnApplyFilters.setOnClickListener {
            val now = Calendar.getInstance()
            selectedMonth = now.get(Calendar.MONTH) + 1
            selectedYear = now.get(Calendar.YEAR)
            binding.spinnerMonth.setText(months[selectedMonth!! - 1], false)
            binding.spinnerYear.setText(selectedYear.toString(), false)
            loadStatement()
        }
        binding.btnPdf.setOnClickListener { openPdf() }
        binding.swipeRefresh.setOnRefreshListener { loadStatement() }
        loadStatement()
    }

    override fun onResume() {
        super.onResume()
        loadStatement()
    }

    private fun setupStats() {
        setupStatCard(AdminStatementStatBinding.bind(binding.statOpening.root), "OPENING", R.drawable.ic_money, "#E3F2FD", "#1976D2")
        setupStatCard(AdminStatementStatBinding.bind(binding.statCredit.root), "CREDITED", R.drawable.ic_payments, "#E8F5E9", "#388E3C")
        setupStatCard(AdminStatementStatBinding.bind(binding.statDebit.root), "DEBITED", R.drawable.ic_bill, "#FCE4EC", "#C2185B")
        setupStatCard(AdminStatementStatBinding.bind(binding.statClosing.root), "CLOSING", R.drawable.ic_cashbook, "#E0F2F1", "#00796B")
    }

    private fun setupStatCard(cardBinding: AdminStatementStatBinding, label: String, iconRes: Int, bgColor: String, iconTint: String) {
        cardBinding.tvStatLabel.text = label
        cardBinding.tvStatValue.setTextColor(Color.parseColor(iconTint))
    }

    private fun setupFilters() {
        val monthAdapter = ArrayAdapter(this, R.layout.delivery_dropdown_item, months)
        binding.spinnerMonth.setAdapter(monthAdapter)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear - 5..currentYear + 1).map { it.toString() }
        binding.spinnerYear.setAdapter(ArrayAdapter(this, R.layout.delivery_dropdown_item, years))

        val currentMonthIndex = Calendar.getInstance().get(Calendar.MONTH)
        binding.spinnerMonth.setText(months[currentMonthIndex], false)
        binding.spinnerYear.setText(currentYear.toString(), false)
        selectedMonth = currentMonthIndex + 1
        selectedYear = currentYear

        binding.spinnerMonth.setOnItemClickListener { _, _, position, _ ->
            selectedMonth = position + 1
            loadStatement()
        }
        binding.spinnerYear.setOnItemClickListener { _, _, position, _ ->
            selectedYear = years[position].toInt()
            loadStatement()
        }
    }

    private fun loadStatement() {
        binding.swipeRefresh.isRefreshing = true
        filterRequest?.cancel()
        filterRequest = lifecycleScope.launch {
            try {
                val response = ApiClient.cashbookApi.getStatement(selectedMonth, selectedYear)
                bindStatement(response)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Toast.makeText(
                    this@AdminStatementActivity,
                    NetworkMessageUtils.friendlyMessage(e, "Failed to load statement"),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
                hideScreenLoading()
            }
        }
    }

    private fun bindStatement(response: StatementResponse) {
        AdminStatementStatBinding.bind(binding.statOpening.root).tvStatValue.text = money(response.summary.opening_balance)
        AdminStatementStatBinding.bind(binding.statCredit.root).tvStatValue.text = money(response.summary.total_credit)
        AdminStatementStatBinding.bind(binding.statDebit.root).tvStatValue.text = money(response.summary.total_debit)
        AdminStatementStatBinding.bind(binding.statClosing.root).tvStatValue.text = money(response.summary.closing_balance)

        binding.statementRows.removeAllViews()
        if (response.transactions.isEmpty()) {
            binding.statementRows.addView(rowText("No statement records found for this month.", true))
        } else {
            var previousDate: String? = null
            response.transactions.forEach { transaction ->
                if (transaction.date != previousDate) {
                    binding.statementRows.addView(sectionHeader(displayDate(transaction.date)))
                    previousDate = transaction.date
                }
                binding.statementRows.addView(transactionRow(transaction))
            }
        }

        addSummarySection("Overall Summary", response.overall_summary)
        addSummarySection("Profit & Loss Summary", response.profit_loss_summary)
    }

    private fun addSummarySection(title: String, rows: List<StatementSummaryRow>) {
        if (rows.isEmpty()) return
        binding.statementRows.addView(sectionHeader(title))
        rows.forEach { row ->
            binding.statementRows.addView(summaryRow(row))
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun displayDate(value: String): String = com.svd.svdagencies.utils.PortalDates.display(value)

    private fun label(value: String, size: Float, color: String, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(when (color) {
            "#17353E" -> this@AdminStatementActivity.getColor(R.color.textColorPrimary)
            "#526670", "#657985" -> this@AdminStatementActivity.getColor(R.color.textColorSecondary)
            "#16704A" -> if ((resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES) Color.parseColor("#97D8B1") else Color.parseColor(color)
            "#AD3449" -> if ((resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES) Color.parseColor("#FFB3C0") else Color.parseColor(color)
            else -> Color.parseColor(color)
        })
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun surface() = com.svd.svdagencies.utils.CompactLinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(14), dp(14), dp(14))
        background = GradientDrawable().apply {
            setColor(this@AdminStatementActivity.getColor(R.color.surface_background))
            cornerRadius = dp(12).toFloat()
        }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }
    }

    private fun transactionRow(transaction: StatementTransaction): View {
        val credit = transaction.direction == "credit"
        return surface().apply {
            addView(label(transaction.type_label, 14f, "#17353E", true))
            addView(label(transaction.particulars, 13f, "#526670").apply {
                setPadding(0, dp(5), 0, 0)
            })
            transaction.reference?.takeIf { it.isNotBlank() }?.let {
                addView(label(it, 11f, "#657985").apply { setPadding(0, dp(4), 0, 0) })
            }
            addView(com.svd.svdagencies.utils.CompactLinearLayout(this@AdminStatementActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(12), 0, 0)
                addView(label(
                    (if (credit) "+ " else "− ") + money(if (credit) transaction.deposit else transaction.withdrawal),
                    16f, if (credit) "#16704A" else "#AD3449", true
                ), LinearLayout.LayoutParams(0, -2, 1f))
                addView(label("Balance " + money(transaction.balance), 12f, "#526670").apply {
                    gravity = Gravity.END
                }, LinearLayout.LayoutParams(0, -2, 1f))
            })
        }
    }

    private fun summaryRow(row: StatementSummaryRow): View = surface().apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(label(row.particulars, 13f, "#526670"), LinearLayout.LayoutParams(0, -2, 1f))
        addView(label(money(row.amount), 14f, "#17353E", true))
    }

    private fun rowText(text: String, empty: Boolean): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(if (empty) this@AdminStatementActivity.getColor(R.color.textColorSecondary) else this@AdminStatementActivity.getColor(R.color.textColorPrimary))
            textSize = if (empty) 14f else 13f
            setPadding(dp(14), dp(16), dp(14), dp(16))
            setBackgroundResource(R.drawable.bg_edit_text)
        }
    }

    private fun sectionHeader(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(this@AdminStatementActivity.getColor(R.color.textColorPrimary))
            textSize = 13f
            setPadding(dp(2), dp(16), dp(2), dp(10))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
    }

    private fun openPdf() {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@AdminStatementActivity, "Generating statement PDF...", Toast.LENGTH_SHORT).show()
                val responseBody = ApiClient.cashbookApi.downloadStatementPdf(selectedMonth, selectedYear)
                val fileName = "statement_${selectedYear}_${selectedMonth}.pdf"
                val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

                withContext(Dispatchers.IO) {
                    responseBody.byteStream().use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                val fileUri = FileProvider.getUriForFile(
                    this@AdminStatementActivity,
                    "${applicationContext.packageName}.provider",
                    file
                )
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(fileUri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                })
            } catch (e: Exception) {
                Toast.makeText(
                    this@AdminStatementActivity,
                    NetworkMessageUtils.friendlyMessage(e, "Failed to generate statement PDF"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(value)
}
