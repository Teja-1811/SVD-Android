package com.svd.svdagencies.ui.customer

import android.content.Intent
import android.os.Bundle
import android.app.DatePickerDialog
import android.view.View
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.svd.svdagencies.utils.WhiteMaterialAlertDialogBuilder
import android.graphics.Color
import android.graphics.Typeface
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.svd.svdagencies.base.BaseActivity
import com.svd.svdagencies.R
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
        title = "Order history"
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
        content.setBackgroundColor(Color.rgb(252,244,245))
        report.setTextColor(Color.rgb(24,59,71))
        history.apply { textSize = 19f; setTypeface(typeface, Typeface.BOLD); setTextColor(Color.rgb(24,59,71)) }
        content.addView(progress)
        val overview = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, 0, padding, padding)
            addView(label("Monthly overview", 18, true))
            addView(report)
            addView(action("Choose month") { chooseReportMonth() })
        }
        content.addView(card(overview))
        val tools = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tools.addView(action("Refresh") { loadReport(); loadHistory() }.apply {
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(8) }
        })
        tools.addView(action("Product brochure") { downloadBrochure() }.apply {
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        })
        content.addView(tools)
        content.addView(history)
        content.addView(orderActions)
        PortalNavigation.installPage(this, ScrollView(this).apply { addView(content) }, "Order history", R.id.nav_history_reports)
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
                if (isFinishing || isDestroyed) return
                progress.visibility = View.GONE
                val value = response.body()
                report.text = if (response.isSuccessful && value != null) {
                    String.format(Locale.US, "%s report\nBills: %d   Total: ₹%.2f   Average: ₹%.2f", value.date, value.totalBills, value.totalAmount, value.averageAmount)
                } else "Unable to load this month's report."
            }
            override fun onFailure(call: Call<CustomerMonthlyReport>, t: Throwable) {
                if (isFinishing || isDestroyed) return
                progress.visibility = View.GONE
                report.text = "Unable to load this month's report."
            }
        })
    }

    private fun loadHistory() {
        progress.visibility = View.VISIBLE
        ApiClient.customerHistoryApi.getOrderHistory().enqueue(object : Callback<CustomerOrderHistoryResponse> {
            override fun onResponse(call: Call<CustomerOrderHistoryResponse>, response: Response<CustomerOrderHistoryResponse>) {
                if (isFinishing || isDestroyed) return
                progress.visibility = View.GONE
                val orders = response.body()?.orders.orEmpty()
                orderActions.removeAllViews()
                history.text = if (response.isSuccessful && orders.isNotEmpty()) {
                    "Order history (${orders.size})"
                } else "No orders found."
                orders.forEach { order -> addOrderActions(order) }
            }
            override fun onFailure(call: Call<CustomerOrderHistoryResponse>, t: Throwable) {
                if (isFinishing || isDestroyed) return
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

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun label(value: String, size: Int = 14, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(Color.rgb(24,59,71))
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun action(value: String, clicked: () -> Unit) = MaterialButton(this).apply {
        text = value; isAllCaps = false; cornerRadius = dp(12); minHeight = dp(48)
        setOnClickListener { clicked() }
    }

    private fun card(body: View) = MaterialCardView(this).apply {
        radius = dp(18).toFloat(); cardElevation = 0f; strokeWidth = dp(1)
        strokeColor = Color.rgb(228,233,236); setCardBackgroundColor(Color.WHITE)
        layoutParams = LinearLayout.LayoutParams(-1,-2).apply { bottomMargin = dp(12) }
        addView(body)
    }

    private fun statusLabel(value: String) = value.replace('_', ' ').replaceFirstChar { it.uppercase() }

    private fun addOrderActions(order: com.svd.svdagencies.data.api.customer.CustomerOrderHistoryItem) {
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16),dp(16),dp(16),dp(12)) }
        val heading = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
        heading.addView(label("₹${String.format(Locale.US, "%.2f", order.grandTotal)}", 22, true).apply {
            layoutParams = LinearLayout.LayoutParams(0,-2,1f)
        })
        heading.addView(label(statusLabel(order.status),12,true).apply {
            setPadding(dp(10),dp(6),dp(10),dp(6))
            setTextColor(if (order.status == "payment_pending") Color.rgb(151,97,0) else Color.rgb(24,100,80))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(if (order.status == "payment_pending") Color.rgb(255,245,217) else Color.rgb(234,246,240))
                cornerRadius = dp(8).toFloat()
            }
        })
        body.addView(heading)
        body.addView(label(order.orderNumber,12).apply { setPadding(0,dp(12),0,dp(8)); setTextColor(Color.rgb(100,121,133)) })
        body.addView(label("Ordered  ${order.orderDate}",14))
        body.addView(label("Delivery  ${order.deliveryDate}",14).apply { setPadding(0,dp(4),0,dp(8)) })
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(action("View details") { showOrderDetails(order) }.apply { layoutParams = LinearLayout.LayoutParams(0,-2,1f) })
        if (order.canDelete) actions.addView(action("Remove") {
            WhiteMaterialAlertDialogBuilder(this).setTitle("Remove order?")
                .setMessage(order.orderNumber).setNegativeButton("Keep",null)
                .setPositiveButton("Remove") { _, _ -> deleteOrder(order.orderId) }.show()
        }.apply {
            setBackgroundColor(Color.WHITE); setTextColor(Color.rgb(205,43,43))
            layoutParams = LinearLayout.LayoutParams(-2,-2).apply { marginStart = dp(8) }
        })
        body.addView(actions)
        orderActions.addView(card(body))
    }

    private fun showOrderDetails(order: com.svd.svdagencies.data.api.customer.CustomerOrderHistoryItem) {
        val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20),dp(20),dp(20),dp(24))
            setBackgroundColor(Color.WHITE)
        }
        fun money(value: Double) = "₹${String.format(Locale.US, "%.2f", value)}"
        body.addView(label("Order details",22,true))
        body.addView(label(order.orderNumber,12).apply {
            setPadding(0,dp(6),0,dp(14)); setTextColor(Color.rgb(100,121,133))
        })
        body.addView(label(statusLabel(order.status),14,true).apply {
            setPadding(dp(12),dp(10),dp(12),dp(10))
            setTextColor(Color.rgb(151,97,0))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.rgb(255,245,217)); cornerRadius = dp(10).toFloat()
            }
        })
        body.addView(label("Ordered: ${order.orderDate}",14).apply { setPadding(0,dp(14),0,dp(4)) })
        body.addView(label("Delivery: ${order.deliveryDate}",14).apply { setPadding(0,0,0,dp(18)) })
        fun tableRow(item: String, qty: String, amount: String, heading: Boolean = false): LinearLayout {
            return LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(10),dp(12),dp(10),dp(12))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(if (heading) Color.rgb(241,245,247) else Color.WHITE)
                    setStroke(dp(1),Color.rgb(228,233,236))
                }
                listOf(item,qty,amount).forEachIndexed { index,value ->
                    addView(label(value,if (heading) 12 else 14,heading || index == 2).apply {
                        gravity = if (index == 0) android.view.Gravity.START else android.view.Gravity.END
                        layoutParams = LinearLayout.LayoutParams(0,-2,when(index) { 0 -> 0.57f; 1 -> 0.13f; else -> 0.30f })
                        if(index == 0) setPadding(0,0,dp(10),0)
                    })
                }
            }
        }
        body.addView(tableRow("Item / unit price","Qty","Amount",true))
        order.items.forEach { line ->
            val pricing = money(line.unitPrice) + " each" + if (line.discount > 0) " · ${money(line.discount)} off/unit" else ""
            body.addView(tableRow("${line.itemName}\n$pricing",line.quantity.toString(),money(line.total)))
        }
        val totals = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0,dp(14),0,dp(12)) }
        fun totalRow(title: String, amount: Double, emphasized: Boolean = false) = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(0,dp(6),0,dp(6))
            addView(label(title,if(emphasized) 19 else 14,emphasized).apply { layoutParams = LinearLayout.LayoutParams(0,-2,1f) })
            addView(label(money(amount),if(emphasized) 19 else 14,true))
        }
        totals.addView(totalRow("Items subtotal",order.items.sumOf { it.total }))
        totals.addView(totalRow("Delivery",order.deliveryCharge))
        totals.addView(totalRow("Total",order.grandTotal,true))
        body.addView(totals)
        body.addView(action("Close") { sheet.dismiss() }.apply { layoutParams = LinearLayout.LayoutParams(-1,-2) })
        sheet.setContentView(ScrollView(this).apply { isFillViewport = true; addView(body) })
        sheet.setOnShowListener {
            sheet.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            sheet.behavior.maxHeight = (resources.displayMetrics.heightPixels * 0.9f).toInt()
        }
        sheet.show()
    }

    private fun deleteOrder(orderId: Int) {
        progress.visibility = View.VISIBLE
        ApiClient.customerHistoryApi.deleteOrder(orderId).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (isFinishing || isDestroyed) return
                progress.visibility = View.GONE
                Toast.makeText(this@CustomerHistoryActivity, if (response.isSuccessful) "Order removed" else "Order cannot be removed", Toast.LENGTH_SHORT).show()
                if (response.isSuccessful) loadHistory()
            }
            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                if (isFinishing || isDestroyed) return
                progress.visibility = View.GONE
                Toast.makeText(this@CustomerHistoryActivity, "Unable to remove order", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun downloadBrochure() {
        progress.visibility = View.VISIBLE
        ApiClient.customerHistoryApi.downloadBrochure().enqueue(object : Callback<okhttp3.ResponseBody> {
            override fun onResponse(call: Call<okhttp3.ResponseBody>, response: Response<okhttp3.ResponseBody>) {
                if (isFinishing || isDestroyed) return
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
                if (isFinishing || isDestroyed) return
                progress.visibility = View.GONE
                Toast.makeText(this@CustomerHistoryActivity, "Unable to download brochure", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
