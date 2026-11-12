package com.svd.svdagencies.ui.delivery

import com.svd.svdagencies.utils.SharedDatePickerDialog as DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.base.BaseActivity
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.delivery.DeliveryCustomerPaymentRecord
import com.svd.svdagencies.databinding.DeliveryCustomerPaymentsBinding
import com.svd.svdagencies.databinding.CustomerPaymentRowBinding
import com.svd.svdagencies.utils.NetworkMessageUtils
import kotlinx.coroutines.launch
import retrofit2.awaitResponse
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DeliveryCustomerPaymentsActivity : BaseActivity() {

    private data class FilterOption(val label: String, val id: Int?)

    private companion object {
        const val TAG = "DeliveryPayments"
    }

    private lateinit var binding: DeliveryCustomerPaymentsBinding
    private val adapter = DeliveryCustomerPaymentAdapter { payment, action -> showPaymentActions(payment, action) }
    private var customerId: Int = -1
    private var customerName: String? = null
    private var selectedRouteId: Int? = null
    private var selectedCustomerId: Int? = null
    private var routes = emptyList<com.svd.svdagencies.data.model.delivery.DeliveryRoute>()
    private var customers = emptyList<com.svd.svdagencies.data.model.delivery.DeliveryBillCustomer>()

    private val selectedMonth: Calendar = Calendar.getInstance()
    private val monthLabelFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DeliveryCustomerPaymentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        customerId = intent.getIntExtra("customer_id", -1)
        customerName = intent.getStringExtra("customer_name")

        setupToolbar()
        setupMonthFilter()
        setupRecyclerView()
        loadFilterOptions()

        binding.swipeRefresh.setOnRefreshListener { loadPayments() }
        loadPayments()
    }

    private fun setupToolbar() {
        binding.toolbar.title = customerName ?: "Payments"
        DeliveryNavigation.setup(
            this,
            binding.deliveryDrawerLayout,
            binding.deliveryNavigationView,
            toolbar = binding.toolbar,
            selectedItemId = if (customerId == -1) R.id.nav_delivery_payments else 0
        )
    }

    private fun setupMonthFilter() {
        updateMonthLabel()
        binding.tvSelectedMonth.setOnClickListener { showMonthPicker() }
        binding.filterRoute.setOnClickListener { selectRoute() }
        binding.filterCustomer.setOnClickListener { selectCustomer() }
    }

    private fun showMonthPicker() {
        DatePickerDialog(
            this,
            { _, year, month, _ ->
                selectedMonth.set(Calendar.YEAR, year)
                selectedMonth.set(Calendar.MONTH, month)
                selectedMonth.set(Calendar.DAY_OF_MONTH, 1)
                updateMonthLabel()
                loadPayments()
            },
            selectedMonth.get(Calendar.YEAR),
            selectedMonth.get(Calendar.MONTH),
            selectedMonth.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateMonthLabel() {
        binding.tvSelectedMonth.text = monthLabelFormat.format(selectedMonth.time)
        binding.tvSelectedMonth.contentDescription = "Filter by month and year"
    }

    private fun loadFilterOptions() = lifecycleScope.launch {
        setFilterLoading(true)
        try {
            routes = ApiClient.deliveryApi.getRoutes().awaitResponse().body().orEmpty()
            customers = ApiClient.deliveryApi.getBillCustomers().awaitResponse().body()?.results.orEmpty()
        } catch (_: Exception) {
            Toast.makeText(this@DeliveryCustomerPaymentsActivity, "Unable to load filters", Toast.LENGTH_SHORT).show()
        } finally {
            setFilterLoading(false)
        }
    }

    private fun setFilterLoading(isLoading: Boolean) {
        binding.filterLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.filterRoute.isEnabled = !isLoading
        binding.filterCustomer.isEnabled = !isLoading
        binding.filterRoute.alpha = if (isLoading) 0.55f else 1f
        binding.filterCustomer.alpha = if (isLoading) 0.55f else 1f
    }

    private fun selectRoute() {
        val options = listOf(FilterOption("All routes", null)) + routes.map {
            FilterOption(it.name, it.id)
        }
        showBorderedDropdown(binding.filterRoute, options) { option ->
            selectedRouteId = option.id
            binding.filterRoute.text = option.label
            selectedCustomerId = null
            binding.filterCustomer.text = "All customers"
            loadPayments()
        }
    }

    private fun selectCustomer() {
        val visibleCustomers = customers.filter { selectedRouteId == null || it.routeId == selectedRouteId }
        val options = listOf(FilterOption("All customers", null)) + visibleCustomers.map {
            FilterOption(it.label, it.id)
        }
        showBorderedDropdown(binding.filterCustomer, options) { option ->
            selectedCustomerId = option.id
            binding.filterCustomer.text = option.label
            loadPayments()
        }
    }

    private fun showBorderedDropdown(
        anchor: View,
        options: List<FilterOption>,
        onSelect: (FilterOption) -> Unit,
    ) {
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density + .5f).toInt()
        val rows = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
            setBackgroundColor(Color.WHITE)
        }
        val scroll = ScrollView(this).apply { addView(rows) }
        val anchorPosition = IntArray(2)
        anchor.getLocationOnScreen(anchorPosition)
        // Size to the content up to a sensible scrollable limit; do not leave a
        // screen-height empty panel beneath a short list of options.
        val popupHeight = minOf(dp(360), dp(8) + options.size * dp(52))
        val popup = PopupWindow(scroll, anchor.width, popupHeight, true).apply {
            setBackgroundDrawable(ColorDrawable(Color.WHITE))
            elevation = dp(8).toFloat()
            isOutsideTouchable = true
        }
        options.forEach { option ->
            rows.addView(android.widget.TextView(this).apply {
                text = option.label
                textSize = 16f
                setTextColor(getColor(R.color.textColorPrimary))
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(14), 0, dp(14), 0)
                background = getDrawable(R.drawable.bg_qty_outline)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)).apply {
                    bottomMargin = dp(4)
                }
                setOnClickListener {
                    popup.dismiss()
                    onSelect(option)
                }
            })
        }
        popup.showAtLocation(
            anchor.rootView,
            android.view.Gravity.TOP or android.view.Gravity.START,
            anchorPosition[0],
            anchorPosition[1] + anchor.height + dp(4),
        )
    }

    private fun setupRecyclerView() {
        binding.rvPayments.layoutManager = LinearLayoutManager(this)
        binding.rvPayments.adapter = adapter
    }

    private fun loadPayments() {
        binding.swipeRefresh.isRefreshing = true
        
        // If customerId is -1 (from menu), use the logged-in agent's user ID
        val targetId = if (customerId != -1) customerId else {
            com.svd.svdagencies.utils.SessionManager(this).getUserId()
        }

        if (targetId == -1) {
            Toast.makeText(this, "User session expired", Toast.LENGTH_SHORT).show()
            binding.swipeRefresh.isRefreshing = false
            return
        }

        lifecycleScope.launch {
            try {
                Log.d(
                    TAG,
                    "Loading payments: month=${selectedMonth.get(Calendar.MONTH) + 1}, " +
                        "year=${selectedMonth.get(Calendar.YEAR)}, route=$selectedRouteId, " +
                        "customer=$selectedCustomerId",
                )
                // Call the correct customer payments URL for both general and specific views
                val response = ApiClient.deliveryApi.getCustomerPaymentRecords(
                    customerId = targetId,
                    month = selectedMonth.get(Calendar.MONTH) + 1,
                    year = selectedMonth.get(Calendar.YEAR),
                    scope = if (customerId == -1) "agent" else null,
                    routeId = selectedRouteId,
                    filterCustomerId = selectedCustomerId
                ).awaitResponse()
                
                if (response.isSuccessful) {
                    val serverPayments = response.body()?.payments.orEmpty()
                    // Older deployed servers do not include these two fields.
                    // Only use the client-side verification once the response
                    // actually supplies the corresponding identifiers.
                    val includesCustomerIds = serverPayments.any { it.customerId != null }
                    val includesRouteIds = serverPayments.any { it.routeId != null }
                    val payments = serverPayments.filter { payment ->
                        (selectedCustomerId == null || !includesCustomerIds || payment.customerId == selectedCustomerId) &&
                            (selectedRouteId == null || !includesRouteIds || payment.routeId == selectedRouteId)
                    }
                    adapter.submitList(payments)
                    binding.emptyState.visibility = if (payments.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    Toast.makeText(
                        this@DeliveryCustomerPaymentsActivity,
                        NetworkMessageUtils.parseError(response, "Failed to load payments"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@DeliveryCustomerPaymentsActivity,
                    NetworkMessageUtils.friendlyMessage(e, "Error loading data"),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showPaymentActions(payment: DeliveryCustomerPaymentRecord, action: Int) {
        run {
                if (action == 0) {
                    val input = android.widget.EditText(this).apply {
                        inputType = android.text.InputType.TYPE_CLASS_NUMBER
                        keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789")
                        setText("%.0f".format(java.util.Locale.US, payment.amount))
                    }
                    com.svd.svdagencies.utils.WhiteMaterialAlertDialogBuilder(this).setTitle("Edit collected amount")
                        .setView(input).setNegativeButton("Cancel", null)
                        .setPositiveButton("Save") { _, _ ->
                            mutatePayment(ApiClient.deliveryApi.editCollectedPayment(payment.id, mapOf("amount" to input.text.toString())))
                        }.show()
                } else {
                    com.svd.svdagencies.utils.WhiteMaterialAlertDialogBuilder(this).setTitle("Delete payment?")
                        .setMessage("This will update the customer's due and your collection balance.")
                        .setNegativeButton("Cancel", null).setPositiveButton("Delete") { _, _ ->
                            mutatePayment(ApiClient.deliveryApi.deleteCollectedPayment(payment.id))
                        }.show()
                }
            }
    }
    private fun mutatePayment(call: retrofit2.Call<okhttp3.ResponseBody>) {
        lifecycleScope.launch {
            try {
                val response = call.awaitResponse()
                if (response.isSuccessful) loadPayments()
                else Toast.makeText(this@DeliveryCustomerPaymentsActivity, NetworkMessageUtils.parseError(response, "Unable to update payment"), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@DeliveryCustomerPaymentsActivity, "Unable to update payment", Toast.LENGTH_LONG).show()
            }
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

class DeliveryCustomerPaymentAdapter(private val onManage: (DeliveryCustomerPaymentRecord, Int) -> Unit = { _, _ -> }) : RecyclerView.Adapter<DeliveryCustomerPaymentAdapter.ViewHolder>() {

    private var items: List<DeliveryCustomerPaymentRecord> = emptyList()
    private val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    private val outputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)

    private fun formatTimestamp(value: String?): String? = value?.let { raw ->
        runCatching { inputFormat.parse(raw)?.let(outputFormat::format) }.getOrNull() ?: raw
    }

    fun submitList(newItems: List<DeliveryCustomerPaymentRecord>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CustomerPaymentRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: CustomerPaymentRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DeliveryCustomerPaymentRecord) {
            binding.paymentActions.visibility = if (item.canManage) View.VISIBLE else View.GONE
            com.svd.svdagencies.utils.CompactUi.applyTree(binding.root)
            binding.paymentEdit.setOnClickListener { onManage(item, 0) }
            binding.paymentDelete.setOnClickListener { onManage(item, 1) }
            binding.root.contentDescription = if (item.canManage) "Payment, tap to edit or delete" else "Payment"
            binding.tvAmount.text = "₹%.2f".format(item.amount)
            
            // For general view, include customer name in the "payment for" label
            val prefix = if (item.customerName != null) "${item.customerName} - " else ""
            binding.tvPaymentFor.text = "$prefix${item.paymentFor ?: "Payment"}"

            binding.tvPaymentMethod.text = "Method: ${item.method ?: "N/A"}"
            binding.tvTransactionId.text = "TXN: ${item.transactionId ?: "N/A"}"
            binding.tvCollectedBy.text = "Collected by: ${item.collectedByName ?: "Not recorded"}"
            val editedBy = item.editedByName?.takeIf { it.isNotBlank() }
            binding.tvEditedBy.visibility = if (editedBy == null) View.GONE else View.VISIBLE
            binding.tvEditedBy.text = if (editedBy == null) "" else {
                "Edited by: $editedBy${formatTimestamp(item.editedAt)?.let { " • $it" }.orEmpty()}"
            }
            binding.tvStatus.text = item.status?.uppercase() ?: "UNKNOWN"

            val statusBg = when (item.status?.lowercase()) {
                "success", "paid", "completed" -> R.drawable.bg_status_green
                "pending", "processing" -> R.drawable.bg_status_yellow
                "failed", "cancelled" -> R.drawable.bg_status_red
                else -> R.drawable.bg_status_yellow
            }
            binding.tvStatus.setBackgroundResource(statusBg)

            try {
                item.createdAt?.let {
                    val date = inputFormat.parse(it)
                    binding.tvPaymentDate.text = outputFormat.format(date!!)
                }
            } catch (e: Exception) {
                binding.tvPaymentDate.text = item.createdAt ?: "N/A"
            }
        }
    }
}
