package com.svd.svdagencies.ui.customer

import android.os.Bundle
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
import com.svd.svdagencies.base.BaseActivity
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.api.customer.CustomerOrderHistoryResponse
import java.util.Locale
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/** Customer-facing list of orders placed by the signed-in customer. */
class CustomerHistoryActivity : BaseActivity() {
    private lateinit var content: LinearLayout
    private lateinit var progress: ProgressBar
    private lateinit var history: TextView
    private lateinit var orderActions: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Order history"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val padding = (16 * resources.displayMetrics.density).toInt()
        content = com.svd.svdagencies.utils.CompactLinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }
        progress = com.svd.svdagencies.utils.LogoProgressBar(this).apply { visibility = View.GONE }
        history = TextView(this).apply { textSize = 13f; setPadding(0, padding, 0, padding) }
        orderActions = com.svd.svdagencies.utils.CompactLinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this@CustomerHistoryActivity, com.svd.svdagencies.R.color.surface_background))
        history.apply { textSize = 16f; setTypeface(typeface, Typeface.BOLD); setTextColor(androidx.core.content.ContextCompat.getColor(this@CustomerHistoryActivity, com.svd.svdagencies.R.color.textColorPrimary)) }
        content.addView(progress)
        content.addView(history)
        content.addView(orderActions)
        PortalNavigation.installPage(this, ScrollView(this, null, android.R.attr.scrollViewStyle).apply { addView(content) }, "Order history", R.id.nav_history_reports)
        loadHistory()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
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
                    "Orders placed (${orders.size})"
                } else "No orders placed yet."
                orders.forEach { order -> addOrderActions(order) }
            }
            override fun onFailure(call: Call<CustomerOrderHistoryResponse>, t: Throwable) {
                if (isFinishing || isDestroyed) return
                progress.visibility = View.GONE
                history.text = "Unable to load order history."
            }
        })
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun label(value: String, size: Int = 14, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(androidx.core.content.ContextCompat.getColor(this@CustomerHistoryActivity, com.svd.svdagencies.R.color.textColorPrimary))
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun action(value: String, clicked: () -> Unit) = MaterialButton(this).apply {
        text = value; isAllCaps = false; cornerRadius = dp(12); minHeight = dp(48)
        setOnClickListener { clicked() }
    }

    private fun card(body: View) = MaterialCardView(this).apply {
        radius = dp(18).toFloat(); cardElevation = 0f; strokeWidth = dp(1)
        strokeColor = androidx.core.content.ContextCompat.getColor(this@CustomerHistoryActivity, com.svd.svdagencies.R.color.card_stroke_color); setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(this@CustomerHistoryActivity, com.svd.svdagencies.R.color.surface_background))
        layoutParams = LinearLayout.LayoutParams(-1,-2).apply { bottomMargin = dp(12) }
        addView(body)
    }

    private fun statusLabel(value: String) = value.replace('_', ' ').replaceFirstChar { it.uppercase() }

    private fun addOrderActions(order: com.svd.svdagencies.data.api.customer.CustomerOrderHistoryItem) {
        val body = com.svd.svdagencies.utils.CompactLinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16),dp(16),dp(16),dp(12)) }
        val heading = com.svd.svdagencies.utils.CompactLinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
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
        body.addView(label(order.orderNumber,12).apply { setPadding(0,dp(12),0,dp(8)); setTextColor(androidx.core.content.ContextCompat.getColor(this@CustomerHistoryActivity, com.svd.svdagencies.R.color.textColorSecondary)) })
        body.addView(label("Ordered  ${order.orderDate}",14))
        body.addView(label("Delivery  ${order.deliveryDate}",14).apply { setPadding(0,dp(4),0,dp(8)) })
        val actions = com.svd.svdagencies.utils.CompactLinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(action("View details") { showOrderDetails(order) }.apply { layoutParams = LinearLayout.LayoutParams(0,-2,1f) })
        val canCancel = order.status.lowercase() in setOf("pending", "payment_pending")
        if (canCancel) actions.addView(action("Cancel order") {
            WhiteMaterialAlertDialogBuilder(this).setTitle("Cancel order?")
                .setMessage("This order has not been confirmed. It will be cancelled and kept in your order history.")
                .setNegativeButton("Keep order",null)
                .setPositiveButton("Cancel order") { _, _ -> cancelOrder(order.orderId) }.show()
        }.apply {
            setBackgroundColor(androidx.core.content.ContextCompat.getColor(this@CustomerHistoryActivity, com.svd.svdagencies.R.color.surface_background)); setTextColor(com.google.android.material.color.MaterialColors.getColor(this@CustomerHistoryActivity, com.google.android.material.R.attr.colorError, getColor(R.color.brand_red)))
            layoutParams = LinearLayout.LayoutParams(-2,-2).apply { marginStart = dp(8) }
        })
        body.addView(actions)
        orderActions.addView(card(body))
    }

    private fun showOrderDetails(order: com.svd.svdagencies.data.api.customer.CustomerOrderHistoryItem) {
        val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val body = com.svd.svdagencies.utils.CompactLinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20),dp(20),dp(20),dp(24))
            setBackgroundColor(androidx.core.content.ContextCompat.getColor(this@CustomerHistoryActivity, com.svd.svdagencies.R.color.surface_background))
        }
        fun money(value: Double) = "₹${String.format(Locale.US, "%.2f", value)}"
        body.addView(label("Order details",22,true))
        body.addView(label(order.orderNumber,12).apply {
            setPadding(0,dp(6),0,dp(14)); setTextColor(androidx.core.content.ContextCompat.getColor(this@CustomerHistoryActivity, com.svd.svdagencies.R.color.textColorSecondary))
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
            return com.svd.svdagencies.utils.CompactLinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(10),dp(12),dp(10),dp(12))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(if (heading) androidx.core.content.ContextCompat.getColor(this@CustomerHistoryActivity, com.svd.svdagencies.R.color.surface_variant) else androidx.core.content.ContextCompat.getColor(this@CustomerHistoryActivity, com.svd.svdagencies.R.color.surface_background))
                    setStroke(dp(1),androidx.core.content.ContextCompat.getColor(this@CustomerHistoryActivity, com.svd.svdagencies.R.color.card_stroke_color))
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
        val totals = com.svd.svdagencies.utils.CompactLinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0,dp(14),0,dp(12)) }
        fun totalRow(title: String, amount: Double, emphasized: Boolean = false) = com.svd.svdagencies.utils.CompactLinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(0,dp(6),0,dp(6))
            addView(label(title,if(emphasized) 19 else 14,emphasized).apply { layoutParams = LinearLayout.LayoutParams(0,-2,1f) })
            addView(label(money(amount),if(emphasized) 19 else 14,true))
        }
        totals.addView(totalRow("Items subtotal",order.items.sumOf { it.total }))
        totals.addView(totalRow("Delivery",order.deliveryCharge))
        totals.addView(totalRow("Total",order.grandTotal,true))
        body.addView(totals)
        body.addView(action("Close") { sheet.dismiss() }.apply { layoutParams = LinearLayout.LayoutParams(-1,-2) })
        sheet.setContentView(ScrollView(this, null, android.R.attr.scrollViewStyle).apply { isFillViewport = true; addView(body) })
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

    private fun cancelOrder(orderId: Int) {
        progress.visibility = View.VISIBLE
        ApiClient.customerHistoryApi.cancelOrder(orderId).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (isFinishing || isDestroyed) return
                progress.visibility = View.GONE
                Toast.makeText(this@CustomerHistoryActivity, if (response.isSuccessful) "Order cancelled" else "Order can no longer be cancelled", Toast.LENGTH_SHORT).show()
                if (response.isSuccessful) loadHistory()
            }
            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                if (isFinishing || isDestroyed) return
                progress.visibility = View.GONE
                Toast.makeText(this@CustomerHistoryActivity, "Unable to cancel order", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
