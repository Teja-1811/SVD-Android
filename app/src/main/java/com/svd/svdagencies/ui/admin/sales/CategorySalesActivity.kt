package com.svd.svdagencies.ui.admin.sales

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.admin.CategorySalesResponse
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CategorySalesActivity : AdminBaseActivity() {
    private lateinit var content: LinearLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var selectedPeriod: String
    private lateinit var selectedDate: Calendar
    private lateinit var dateButton: MaterialButton
    private lateinit var appliedRange: TextView
    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_category_sales_dashboard)
        setupAdminLayout("Category Sales")
        content = findViewById(R.id.categorySalesContent)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        selectedDate = Calendar.getInstance()
        selectedPeriod = "year"
        dateButton = findViewById(R.id.btnSelectSalesDate)
        appliedRange = findViewById(R.id.tvAppliedSalesRange)
        val periods = listOf("Year", "Month", "Week", "Day")
        findViewById<Spinner>(R.id.spinnerPeriod).apply {
            adapter = ArrayAdapter(this@CategorySalesActivity, R.layout.delivery_dropdown_item, periods)
            setSelection(0)
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    selectedPeriod = periods[position].lowercase(Locale.US)
                    load()
                }
            }
        }
        dateButton.setOnClickListener { showDatePicker() }
        swipeRefresh.setOnRefreshListener { load() }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate.set(year, month, day)
                load()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    private fun load() {
        val range = selectedRange()
        dateButton.text = "Anchor date: ${displayDateFormat.format(selectedDate.time)}"
        appliedRange.text = "Showing ${displayDateFormat.format(range.first.time)} – ${displayDateFormat.format(range.second.time)}"
        ApiClient.categorySalesApi.getSummary(
            period = selectedPeriod,
            startDate = apiDateFormat.format(range.first.time),
            endDate = apiDateFormat.format(range.second.time),
        ).enqueue(object : Callback<CategorySalesResponse> {
        override fun onResponse(call: Call<CategorySalesResponse>, response: Response<CategorySalesResponse>) {
            swipeRefresh.isRefreshing = false
            if (!response.isSuccessful) return failure()
            val data = response.body() ?: return failure()
            content.removeAllViews()
            content.addView(TextView(this@CategorySalesActivity).apply { text = "Total volume: %.3f L".format(data.total_volume); textSize = 20f })
            data.categories.forEach { category ->
                content.addView(TextView(this@CategorySalesActivity).apply { text = "$category  •  %.3f L  •  ₹%.2f".format(data.data_by_category[category] ?: 0.0, data.amount_by_category[category] ?: 0.0); textSize = 16f; setPadding(0, 16, 0, 16) })
            }
        }
        override fun onFailure(call: Call<CategorySalesResponse>, error: Throwable) { swipeRefresh.isRefreshing = false; failure() }
        })
    }

    private fun selectedRange(): Pair<Calendar, Calendar> {
        val start = selectedDate.clone() as Calendar
        val end = selectedDate.clone() as Calendar
        when (selectedPeriod) {
            "year" -> {
                start.set(Calendar.MONTH, Calendar.JANUARY)
                start.set(Calendar.DAY_OF_MONTH, 1)
                end.set(Calendar.MONTH, Calendar.DECEMBER)
                end.set(Calendar.DAY_OF_MONTH, 31)
            }
            "month" -> {
                start.set(Calendar.DAY_OF_MONTH, 1)
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            "week" -> {
                start.firstDayOfWeek = Calendar.MONDAY
                start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                end.timeInMillis = start.timeInMillis
                end.add(Calendar.DAY_OF_MONTH, 6)
            }
        }
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)
        end.set(Calendar.HOUR_OF_DAY, 0)
        end.set(Calendar.MINUTE, 0)
        end.set(Calendar.SECOND, 0)
        end.set(Calendar.MILLISECOND, 0)
        return start to end
    }

    private fun failure() = Toast.makeText(this, "Unable to load category sales", Toast.LENGTH_SHORT).show()
}
