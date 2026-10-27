package com.svd.svdagencies.ui.admin.sales

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.admin.CategorySalesResponse
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CategorySalesActivity : AdminBaseActivity() {
    private lateinit var content: LinearLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var selectedDate: Calendar
    private var selectedPeriod = "year"
    private lateinit var dateButton: MaterialButton
    private lateinit var appliedRange: TextView
    private lateinit var filterHelp: TextView
    private var activeCall: Call<CategorySalesResponse>? = null
    private val displayDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val apiDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val money = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private val periods = listOf("year", "month", "week", "day")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_category_sales_dashboard)
        setupAdminLayout("Category Sales")
        content = findViewById(R.id.categorySalesContent)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        dateButton = findViewById(R.id.btnSelectSalesDate)
        appliedRange = findViewById(R.id.tvAppliedSalesRange)
        filterHelp = findViewById(R.id.tvSalesFilterHelp)
        selectedPeriod = savedInstanceState?.getString("salesPeriod")?.takeIf { it in periods } ?: "year"
        selectedDate = Calendar.getInstance().apply {
            savedInstanceState?.getLong("salesDate")?.let { timeInMillis = it }
        }
        findViewById<Spinner>(R.id.spinnerPeriod).apply {
            adapter = object : ArrayAdapter<String>(this@CategorySalesActivity, R.layout.delivery_dropdown_item,
                listOf("Whole year", "Whole month", "Week · Mon to Sun", "Single day")) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                    super.getView(position, convertView, parent).apply { background = null }
                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
                    super.getDropDownView(position, convertView, parent).apply { background = null }
            }
            setSelection(periods.indexOf(selectedPeriod))
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (selectedPeriod != periods[position]) {
                        selectedPeriod = periods[position]
                        load()
                    }
                }
            }
        }
        dateButton.setOnClickListener { showDatePicker() }
        swipeRefresh.setOnRefreshListener { load() }
        load()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("salesPeriod", selectedPeriod)
        outState.putLong("salesDate", selectedDate.timeInMillis)
        super.onSaveInstanceState(outState)
    }

    private fun showDatePicker() {
        DatePickerDialog(this, { _, year, month, day ->
            selectedDate.set(year, month, day)
            load()
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun load() {
        activeCall?.cancel()
        val range = CategorySalesRange.forDate(selectedPeriod, selectedDate)
        val rangeText = if (selectedPeriod == "day") displayDate.format(range.first.time)
            else "${displayDate.format(range.first.time)} – ${displayDate.format(range.second.time)}"
        dateButton.text = when (selectedPeriod) {
            "year" -> SimpleDateFormat("yyyy", Locale.getDefault()).format(selectedDate.time)
            "month" -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(selectedDate.time)
            else -> displayDate.format(selectedDate.time)
        }
        dateButton.contentDescription = "Change sales ${selectedPeriod}: ${dateButton.text}"
        filterHelp.text = when (selectedPeriod) {
            "year" -> "Choose any date in the year you want to view. The whole year is included."
            "month" -> "Choose any date in the month you want to view. The whole month is included."
            "week" -> "Choose any date in the week you want to view. Monday to Sunday is included."
            else -> "Choose the exact day you want to view. Only that day's sales are included."
        } + " Results update automatically."
        appliedRange.text = "Loading: $rangeText"
        content.removeAllViews()
        content.addView(label("Loading category sales…", 16f))
        swipeRefresh.isRefreshing = true
        val request = ApiClient.categorySalesApi.getSummary(selectedPeriod,
            apiDate.format(range.first.time), apiDate.format(range.second.time))
        activeCall = request
        request.enqueue(object : Callback<CategorySalesResponse> {
            override fun onResponse(call: Call<CategorySalesResponse>, response: Response<CategorySalesResponse>) {
                if (call !== activeCall) return
                swipeRefresh.isRefreshing = false
                val data = response.body()
                if (!response.isSuccessful || data == null) { showFailure(rangeText); return }
                appliedRange.text = "Showing: $rangeText"
                render(data)
            }
            override fun onFailure(call: Call<CategorySalesResponse>, error: Throwable) {
                if (call !== activeCall || call.isCanceled) return
                swipeRefresh.isRefreshing = false
                showFailure(rangeText)
            }
        })
    }

    private fun render(data: CategorySalesResponse) {
        content.removeAllViews()
        val totalAmount = data.amount_by_category.values.sum()
        val names = (data.categories + data.amount_by_category.keys + data.data_by_category.keys).distinct()
        card().apply {
            addView(label("Sales overview", 18f, true))
            addView(label("Sales amount", 14f, gap = 16))
            addView(label(money.format(totalAmount), 28f, true, primary = true))
            addView(label("Volume sold", 14f, gap = 16))
            addView(label(volume(data.total_volume), 22f, true))
            addView(label("Sales amount is the value of billed items, not payment collections. Volume is calculated from product pack sizes; an item can have sales even when its volume is 0 L.", 13f, gap = 12).apply { visibility = android.view.View.GONE })
        }
        val hasSales = totalAmount != 0.0 || data.total_volume != 0.0 ||
            data.amount_by_category.values.any { it != 0.0 } || data.data_by_category.values.any { it != 0.0 }
        if (!hasSales) {
            card().apply {
                addView(label("No sales in this period", 18f, true))
                addView(label("Try a different date or choose a wider period above.", 14f, gap = 8).apply { visibility = android.view.View.GONE })
            }
            return
        }
        content.addView(label("Sales by category", 20f, true, gap = 8))
        content.addView(label("Compare each category's billed amount and volume.", 14f, gap = 4).apply { visibility = android.view.View.GONE })
        names.forEach { name ->
            card().apply {
                addView(label(name, 18f, true))
                val stacked = resources.configuration.screenWidthDp < 360 || resources.configuration.fontScale > 1.2f
                val row = com.svd.svdagencies.utils.CompactLinearLayout(this@CategorySalesActivity).apply {
                    orientation = if (stacked) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) }
                }
                val amount = com.svd.svdagencies.utils.CompactLinearLayout(this@CategorySalesActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(if (stacked) -1 else 0, -2, if (stacked) 0f else 1f).apply { marginEnd = if (stacked) 0 else dp(12) }
                    addView(label("Sales amount", 13f))
                    addView(label(money.format(data.amount_by_category[name] ?: 0.0), 20f, true, primary = true))
                }
                val litres = com.svd.svdagencies.utils.CompactLinearLayout(this@CategorySalesActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(if (stacked) -1 else 0, -2, if (stacked) 0f else 1f).apply { topMargin = if (stacked) dp(12) else 0 }
                    addView(label("Volume sold", 13f))
                    addView(label(volume(data.data_by_category[name] ?: 0.0), 20f, true))
                }
                row.addView(amount); row.addView(litres); addView(row)
            }
        }
    }

    private fun showFailure(rangeText: String) {
        appliedRange.text = "Sales unavailable: $rangeText"
        content.removeAllViews()
        card().apply {
            addView(label("Couldn't load sales", 18f, true))
            addView(label("Check your connection and try again. Your selected period and date have been kept.", 14f, gap = 8).apply { visibility = android.view.View.GONE })
            addView(MaterialButton(this@CategorySalesActivity).apply {
                text = "Try again"; isAllCaps = false; minHeight = dp(48)
                setOnClickListener { load() }
            })
        }
    }

    private fun card(): LinearLayout {
        val card = MaterialCardView(this).apply {
            radius = dp(16).toFloat(); cardElevation = 0f; strokeWidth = dp(1)
            strokeColor = ContextCompat.getColor(this@CategorySalesActivity, R.color.card_stroke_color)
            setCardBackgroundColor(ContextCompat.getColor(this@CategorySalesActivity, R.color.card_background))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
        }
        val body = com.svd.svdagencies.utils.CompactLinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        card.addView(body); content.addView(card)
        return body
    }

    private fun label(value: String, size: Float, bold: Boolean = false, gap: Int = 0, primary: Boolean = false) = TextView(this).apply {
        text = value; textSize = size
        setTextColor(if (primary) com.google.android.material.color.MaterialColors.getColor(this@CategorySalesActivity,
            com.google.android.material.R.attr.colorPrimary, 0) else ContextCompat.getColor(this@CategorySalesActivity,
            if (bold) R.color.textColorPrimary else R.color.textColorSecondary))
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(gap) }
    }

    private fun volume(value: Double) = String.format(Locale.getDefault(), "%,.3f L", value)
    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()

    override fun onDestroy() {
        activeCall?.cancel()
        activeCall = null
        super.onDestroy()
    }
}
