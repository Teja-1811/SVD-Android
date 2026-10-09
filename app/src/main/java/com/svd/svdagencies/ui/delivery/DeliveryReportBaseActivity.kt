package com.svd.svdagencies.ui.delivery

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.svd.svdagencies.R
import com.svd.svdagencies.base.BaseActivity
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.delivery.DeliveryAgentDuesResponse
import com.svd.svdagencies.databinding.AdminStatCardBinding
import com.svd.svdagencies.utils.AppSwipeRefreshLayout
import com.svd.svdagencies.utils.NetworkMessageUtils
import kotlinx.coroutines.launch
import retrofit2.awaitResponse
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

abstract class DeliveryReportBaseActivity : BaseActivity() {
    protected abstract val screenTitle: String
    protected abstract val selectedNavItem: Int
    protected abstract val sectionTitle: String

    private lateinit var swipeRefresh: AppSwipeRefreshLayout
    private lateinit var listContainer: LinearLayout
    private lateinit var selectedMonthLabel: TextView
    private val selectedMonth: Calendar = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val monthLabelFormat = SimpleDateFormat("MMMM yyyy", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.delivery_report)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.title = screenTitle
        DeliveryNavigation.setup(
            this,
            findViewById(R.id.deliveryDrawerLayout),
            findViewById(R.id.deliveryNavigationView),
            toolbar = toolbar,
            selectedItemId = selectedNavItem
        )

        swipeRefresh = findViewById(R.id.swipeRefresh)
        listContainer = findViewById(R.id.listContainer)
        selectedMonthLabel = findViewById(R.id.tvSelectedMonth)
        findViewById<TextView>(R.id.tvSectionTitle).text = sectionTitle
        setupMonthFilter()
        swipeRefresh.setOnRefreshListener { loadReport() }
        loadReport()
    }

    private fun setupMonthFilter() {
        updateMonthLabel()
        findViewById<View>(R.id.btnPreviousMonth).setOnClickListener {
            selectedMonth.add(Calendar.MONTH, -1)
            selectedMonth.set(Calendar.DAY_OF_MONTH, 1)
            updateMonthLabel()
            loadReport()
        }
        findViewById<View>(R.id.btnNextMonth).setOnClickListener {
            selectedMonth.add(Calendar.MONTH, 1)
            selectedMonth.set(Calendar.DAY_OF_MONTH, 1)
            updateMonthLabel()
            loadReport()
        }
        selectedMonthLabel.setOnClickListener { showMonthPicker() }
    }

    private fun showMonthPicker() {
        DatePickerDialog(
            this,
            { _, year, month, _ ->
                selectedMonth.set(Calendar.YEAR, year)
                selectedMonth.set(Calendar.MONTH, month)
                selectedMonth.set(Calendar.DAY_OF_MONTH, 1)
                updateMonthLabel()
                loadReport()
            },
            selectedMonth.get(Calendar.YEAR),
            selectedMonth.get(Calendar.MONTH),
            selectedMonth.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateMonthLabel() {
        selectedMonthLabel.text = monthLabelFormat.format(selectedMonth.time)
    }

    protected open fun bindReport(response: DeliveryAgentDuesResponse) {
        bindStat(R.id.statBills, "Bills Generated", response.summary.billCount.toString(), "#E3F2FD", R.drawable.ic_bill)
        bindStat(R.id.statSubmit, "Invoice Amount", money(response.summary.totalAmount), "#E8F5E9", R.drawable.ic_money)
        bindStat(R.id.statDue, "Paid Amount", money(response.summary.totalPaid), "#E0F2F1", R.drawable.ic_payments)
        bindStat(R.id.statProfit, "Current Due", money(currentAgentDue(response)), "#FFEBEE", R.drawable.ic_warning)
        bindStat(R.id.statSalaryPaid, "Salary Earned", money(salaryEarned(response)), "#F1F8E9", R.drawable.ic_graph)
        bindStat(R.id.statSalaryDue, "Pending Salary", money(pendingSalary(response)), "#FFF3E0", R.drawable.ic_orders)

        renderContent(response)
    }

    protected open fun renderContent(response: DeliveryAgentDuesResponse) {
        renderTextRows(renderRows(response))
    }

    protected abstract fun renderRows(response: DeliveryAgentDuesResponse): List<Pair<String, String>>

    private fun loadReport() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val response = ApiClient.deliveryApi.getAgentSummary(
                    startDate = monthStartDate(),
                    endDate = monthEndDate()
                ).awaitResponse()
                if (response.isSuccessful) {
                    response.body()?.let {
                        bindReport(it)
                    }
                } else {
                    Toast.makeText(
                        this@DeliveryReportBaseActivity,
                        NetworkMessageUtils.parseError(response, "Failed to load report"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@DeliveryReportBaseActivity,
                    NetworkMessageUtils.friendlyMessage(e, "Failed to load report"),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun bindStat(id: Int, label: String, value: String, colorHex: String, iconRes: Int) {
        val statCard = findViewById<ViewGroup>(id) ?: return
        val binding = AdminStatCardBinding.bind(statCard)
        binding.tvStatLabel.text = label
        binding.tvStatValue.text = value
        binding.ivStatIcon.setImageResource(iconRes)
        
        val color = Color.parseColor(colorHex)
        binding.cardIcon.setCardBackgroundColor(color)
        
        // Generate a darker shade for the icon tint
        val darkerColor = Color.argb(
            255,
            (Color.red(color) * 0.6).toInt(),
            (Color.green(color) * 0.6).toInt(),
            (Color.blue(color) * 0.6).toInt()
        )
        binding.ivStatIcon.imageTintList = android.content.res.ColorStateList.valueOf(darkerColor)
    }

    private fun renderTextRows(rows: List<Pair<String, String>>) {
        listContainer.removeAllViews()
        if (rows.isEmpty()) {
            val tv = TextView(this)
            tv.text = "No records found."
            tv.setPadding(32, 32, 32, 32)
            tv.gravity = android.view.Gravity.CENTER
            listContainer.addView(tv)
            return
        }

        for (row in rows) {
            val view = layoutInflater.inflate(R.layout.admin_m_row_summary_item, listContainer, false)
            view.findViewById<TextView>(R.id.tvLabel).text = row.first
            view.findViewById<TextView>(R.id.tvValue).text = row.second
            listContainer.addView(view)
        }
    }

    protected fun money(value: Double): String = "Rs. %.2f".format(value)
    protected fun submittedAmount(response: DeliveryAgentDuesResponse): Double {
        return when {
            response.summary.submittedAmount > 0.0 -> response.summary.submittedAmount
            response.summary.counterSubmitAmount > 0.0 -> response.summary.counterSubmitAmount
            else -> 0.0
        }
    }

    protected fun needToSubmitAmount(response: DeliveryAgentDuesResponse): Double {
        return when {
            response.summary.amountToSubmit > 0.0 -> response.summary.amountToSubmit
            response.summary.needToSubmitAmount > 0.0 -> response.summary.needToSubmitAmount
            else -> response.summary.collectedAmount
        }
    }

    protected fun counterSubmitAmount(response: DeliveryAgentDuesResponse): Double = needToSubmitAmount(response)

    protected fun currentAgentDue(response: DeliveryAgentDuesResponse): Double {
        return if (response.summary.agentCurrentDue != 0.0) response.summary.agentCurrentDue else response.summary.totalDue
    }

    protected fun salaryEarned(response: DeliveryAgentDuesResponse): Double {
        return if (response.summary.salaryEarned != 0.0) response.summary.salaryEarned else response.summary.totalProfit
    }

    protected fun pendingSalary(response: DeliveryAgentDuesResponse): Double {
        return (salaryEarned(response) - response.summary.salaryPaid).coerceAtLeast(0.0)
    }

    protected fun remainingGeneratedAmount(response: DeliveryAgentDuesResponse): Double {
        return if (response.summary.remainingGeneratedAmount > 0.0) {
            response.summary.remainingGeneratedAmount
        } else {
            (needToSubmitAmount(response) - submittedAmount(response)).coerceAtLeast(0.0)
        }
    }

    private fun monthStartDate(): String {
        val calendar = selectedMonth.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        return apiDateFormat.format(calendar.time)
    }

    private fun monthEndDate(): String {
        val calendar = selectedMonth.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        return apiDateFormat.format(calendar.time)
    }
}

