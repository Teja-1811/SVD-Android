package com.svd.svdagencies.ui.customer

import android.content.Intent
import android.os.Bundle
import android.app.DatePickerDialog
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.svd.svdagencies.base.BaseActivity
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.api.customer.CustomerMonthlyReport
import com.svd.svdagencies.data.api.customer.CustomerOrderHistoryResponse
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/** Customer-facing equivalent of the website's history, reports, and brochure pages. */
class CustomerHistoryActivity : BaseActivity() {
    private lateinit var content: LinearLayout
    private lateinit var progress: ProgressBar
    private lateinit var report: TextView
    private lateinit var history: TextView
    private lateinit var orderActions: LinearLayout
    private var selectedMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Orders & reports"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val padding = (16 * resources.displayMetrics.density).toInt()
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }
        progress = ProgressBar(this).apply { visibility = View.GONE }
        report = TextView(this).apply { textSize = 17f; setPadding(0, padding, 0, padding) }
        history = TextView(this).apply { textSize = 15f; setPadding(0, padding, 0, padding) }
        orderActions = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(TextView(this).apply { text = "Orders & reports"; textSize = 22f })
        content.addView(progress)
        content.addView(report)
        content.addView(Button(this).apply { text = "Choose report month"; setOnClickListener { chooseReportMonth() } })
        content.addView(Button(this).apply { text = "Refresh order history"; setOnClickListener { loadHistory() } })
        content.addView(Button(this).apply { text = "Download product brochure"; setOnClickListener { downloadBrochure() } })
        content.addView(history)
        content.addView(orderActions)
        setContentView(ScrollView(this).apply { addView(content) })
        loadReport()
        loadHistory()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadReport() {
        progress.visibility = View.VISIBLE
        ApiClient.customerHistoryApi.getMonthlyReport(selectedMonth).enqueue(object : Callback<CustomerMonthlyReport> {
            override fun onResponse(call: Call<CustomerMonthlyReport>, response: Response<CustomerMonthlyReport>) {
                progress.visibility = View.GONE
                val value = response.body()
                report.text = if (response.isSuccessful && value != null) {
                    String.format(Locale.US, "%s report\\nBills: %d   Total: ₹%.2f   Average: ₹%.2f", value.date, value.totalBills, value.totalAmount, value.averageAmount)
                } else "Unable to load this month's report."
            }
            override fun onFailure(call: Call<CustomerMonthlyReport>, t: Throwable) {
                progress.visibility = View.GONE
                report.text = "Unable to load this month's report."
            }
        })
    }

    private fun loadHistory() {
        progress.visibility = View.VISIBLE
        ApiClient.customerHistoryApi.getOrderHistory().enqueue(object : Callback<CustomerOrderHistoryResponse> {
            override fun onResponse(call: Call<CustomerOrderHistoryResponse>, response: Response<CustomerOrderHistoryResponse>) {
                progress.visibility = View.GONE
                val orders = response.body()?.orders.orEmpty()
                orderActions.removeAllViews()
                history.text = if (response.isSuccessful && orders.isNotEmpty()) {
                    "Order history (${orders.size})"
                } else "No orders found."
                orders.forEach { order -> addOrderActions(order) }
            }
            override fun onFailure(call: Call<CustomerOrderHistoryResponse>, t: Throwable) {
                progress.visibility = View.GONE
                history.text = "Unable to load order history."
            }
        })
    }

    private fun chooseReportMonth() {
        val calendar = Calendar.getInstance()
        selectedMonth.split("-").let { parts ->
            calendar.set(Calendar.YEAR, parts[0].toInt())
            calendar.set(Calendar.MONTH, parts[1].toInt() - 1)
        }
        DatePickerDialog(this, { _, year, month, _ ->
            selectedMonth = String.format(Locale.US, "%04d-%02d", year, month + 1)
            loadReport()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), 1).show()
    }

    private fun addOrderActions(order: com.svd.svdagencies.data.api.customer.CustomerOrderHistoryItem) {
        val padding = (8 * resources.displayMetrics.density).toInt()
        orderActions.addView(Button(this).apply {
            text = "${order.orderNumber} • ${order.status} • ₹${String.format(Locale.US, "%.2f", order.grandTotal)}"
            isAllCaps = false
            setOnClickListener { showOrderDetails(order) }
            setPadding(padding, padding, padding, padding)
        })
        if (order.canDelete) {
            orderActions.addView(Button(this).apply {
                text = "Remove ${order.orderNumber}"
                isAllCaps = false
                setOnClickListener { deleteOrder(order.orderId) }
            })
        }
    }

    private fun showOrderDetails(order: com.svd.svdagencies.data.api.customer.CustomerOrderHistoryItem) {
        history.text = buildString {
            append("${order.orderNumber} • ${order.status}\\n")
            append("Ordered: ${order.orderDate}  Delivery: ${order.deliveryDate}\\n")
            append("Total: ₹${String.format(Locale.US, "%.2f", order.grandTotal)}\\n\\nItems\\n")
            order.items.forEach { line ->
                append("${line.itemName} × ${line.quantity} — ₹${String.format(Locale.US, "%.2f", line.total)}\\n")
            }
        }
    }

    private fun deleteOrder(orderId: Int) {
        progress.visibility = View.VISIBLE
        ApiClient.customerHistoryApi.deleteOrder(orderId).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                progress.visibility = View.GONE
                Toast.makeText(this@CustomerHistoryActivity, if (response.isSuccessful) "Order removed" else "Order cannot be removed", Toast.LENGTH_SHORT).show()
                if (response.isSuccessful) loadHistory()
            }
            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                progress.visibility = View.GONE
                Toast.makeText(this@CustomerHistoryActivity, "Unable to remove order", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun downloadBrochure() {
        progress.visibility = View.VISIBLE
        ApiClient.customerHistoryApi.downloadBrochure().enqueue(object : Callback<okhttp3.ResponseBody> {
            override fun onResponse(call: Call<okhttp3.ResponseBody>, response: Response<okhttp3.ResponseBody>) {
                progress.visibility = View.GONE
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    Toast.makeText(this@CustomerHistoryActivity, "Unable to download brochure", Toast.LENGTH_SHORT).show()
                    return
                }
                val downloadDir = getExternalFilesDir(null)
                if (downloadDir == null) {
                    Toast.makeText(this@CustomerHistoryActivity, "Storage is unavailable", Toast.LENGTH_SHORT).show()
                    return
                }
                val file = File(downloadDir, "product_brochure.pdf")
                body.byteStream().use { input -> file.outputStream().use { input.copyTo(it) } }
                val uri = FileProvider.getUriForFile(this@CustomerHistoryActivity, "$packageName.provider", file)
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                })
            }
            override fun onFailure(call: Call<okhttp3.ResponseBody>, t: Throwable) {
                progress.visibility = View.GONE
                Toast.makeText(this@CustomerHistoryActivity, "Unable to download brochure", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
