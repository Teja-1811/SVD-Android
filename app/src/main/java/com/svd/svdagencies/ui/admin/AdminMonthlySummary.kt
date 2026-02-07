package com.svd.svdagencies.ui.admin

import android.app.DatePickerDialog
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.admin.CustomerDashboardApi
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.customerData.CustomerItem
import com.svd.svdagencies.data.model.admin.customerData.MonthlySummaryResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AdminMonthlySummary : AdminBaseActivity() {

    private lateinit var autoCustomer: AutoCompleteTextView
    private lateinit var etMonth: MaterialButton
    private lateinit var btnDownload: MaterialButton
    private lateinit var api: CustomerDashboardApi

    private lateinit var layoutResults: LinearLayout
    private lateinit var cardCustomerDetails: MaterialCardView
    private lateinit var cardFinancialSummary: MaterialCardView
    private lateinit var cardVolumeSummary: MaterialCardView

    private lateinit var tvDetailName: TextView
    private lateinit var tvDetailShop: TextView
    private lateinit var tvDetailPhone: TextView
    
    private var customersList: List<CustomerItem> = emptyList()
    private var selectedCustomerId: Int? = null
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_customer_monthly_summary)
        setupAdminLayout("Monthly Summary")

        api = ApiClient.retrofit.create(CustomerDashboardApi::class.java)

        // Initialize views with null safety
        autoCustomer = findViewById(R.id.autoCustomer) ?: return
        etMonth = findViewById(R.id.etMonth) ?: return
        btnDownload = findViewById(R.id.btnDownload) ?: return

        layoutResults = findViewById(R.id.layoutResults) ?: return
        cardCustomerDetails = findViewById(R.id.cardCustomerDetails) ?: return
        cardFinancialSummary = findViewById(R.id.cardFinancialSummary) ?: return
        cardVolumeSummary = findViewById(R.id.cardVolumeSummary) ?: return

        tvDetailName = findViewById(R.id.tvDetailName) ?: return
        tvDetailShop = findViewById(R.id.tvDetailShop) ?: return
        tvDetailPhone = findViewById(R.id.tvDetailPhone) ?: return

        // Set default month
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        etMonth.text = sdf.format(calendar.time)

        etMonth.setOnClickListener { showMonthPicker() }
        
        loadCustomers()

        btnDownload.setOnClickListener {
            downloadPdf()
        }
    }

    private fun showMonthPicker() {
        val dpd = DatePickerDialog(this, { _, year, month, _ ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            etMonth.text = sdf.format(calendar.time)
            
            // Auto fetch summary on date change
            fetchSummary()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        
        dpd.show()
    }

    private fun loadCustomers() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.getCustomers().awaitResponse()
                if (response.isSuccessful) {
                    customersList = response.body()?.customers ?: emptyList()
                    val names = customersList.map { it.name ?: "Unknown" }
                    withContext(Dispatchers.Main) {
                        if (isDestroyed || isFinishing) return@withContext
                        val adapter = ArrayAdapter(this@AdminMonthlySummary, android.R.layout.simple_dropdown_item_1line, names)
                        autoCustomer.setAdapter(adapter)
                        autoCustomer.setOnItemClickListener { _, _, position, _ ->
                            val selectedName = adapter.getItem(position)
                            val customer = customersList.find { it.name == selectedName }
                            selectedCustomerId = customer?.id
                            
                            // Pre-fill customer details from list
                            customer?.let {
                                layoutResults.visibility = View.VISIBLE
                                cardCustomerDetails.visibility = View.VISIBLE
                                tvDetailName.text = it.name
                                tvDetailShop.text = it.shop_name
                                tvDetailPhone.text = it.phone
                            }
                            
                            fetchSummary()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isDestroyed || isFinishing) return@withContext
                    Toast.makeText(this@AdminMonthlySummary, "Failed to load customers", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchSummary() {
        val date = etMonth.text.toString()
        val customerId = selectedCustomerId ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.getMonthlySalesSummary(date, customerId)
                withContext(Dispatchers.Main) {
                    if (isDestroyed || isFinishing) return@withContext
                    updateUI(response)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Toast.makeText(this@AdminMonthlySummary, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun downloadPdf() {
        val customerId = selectedCustomerId
        if (customerId == null) {
            Toast.makeText(this, "Please select a customer first", Toast.LENGTH_SHORT).show()
            return
        }

        val date = etMonth.text.toString() // yyyy-MM

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val responseBody = api.downloadMonthlySalesPdf(date, customerId)
                val fileName = "Monthly_Summary_${date}_$customerId.pdf"
                val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                
                responseBody.byteStream().use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                withContext(Dispatchers.Main) {
                    if (isDestroyed || isFinishing) return@withContext
                    Toast.makeText(this@AdminMonthlySummary, "PDF Downloaded: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isDestroyed || isFinishing) return@withContext
                    Toast.makeText(this@AdminMonthlySummary, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateUI(data: MonthlySummaryResponse) {
        layoutResults.visibility = View.VISIBLE
        cardCustomerDetails.visibility = View.VISIBLE
        cardFinancialSummary.visibility = View.VISIBLE
        cardVolumeSummary.visibility = View.VISIBLE

        // Update details from API response
        tvDetailName.text = data.customer.name
        tvDetailPhone.text = data.customer.phone

        // Financial Summary
        setupRow(findViewById(R.id.rowOpeningDue), "Opening Due", "₹${String.format("%.2f", data.summary.opening_due)}")
        setupRow(findViewById(R.id.rowTotalSales), "Total Sales", "₹${String.format("%.2f", data.summary.total_sales)}")
        setupRow(findViewById(R.id.rowPaidAmount), "Paid Amount", "₹${String.format("%.2f", data.summary.paid_amount)}")
        setupRow(findViewById(R.id.rowDueAmount), "Net Due", "₹${String.format("%.2f", data.summary.due_amount)}")
        setupRow(findViewById(R.id.rowCommission), "Total Commission", "- ₹${String.format("%.2f", data.commission.total_commission)}")
        setupRow(findViewById(R.id.rowRemainingDue), "Remaining Balance", "₹${String.format("%.2f", data.summary.remaining_due)}", true)

        // Volume Summary
        setupRow(findViewById(R.id.rowMilkVolume), "Milk Volume", "${String.format("%.2f", data.volume.milk_volume)} L")
        setupRow(findViewById(R.id.rowCurdVolume), "Curd Volume", "${String.format("%.2f", data.volume.curd_volume)} L")
        setupRow(findViewById(R.id.rowAvgMilk), "Avg Milk/Day", "${String.format("%.2f", data.volume.avg_milk_per_day)} L")
        setupRow(findViewById(R.id.rowAvgCurd), "Avg Curd/Day", "${String.format("%.2f", data.volume.avg_curd_per_day)} L")
    }

    private fun setupRow(view: View?, label: String, value: String, isBold: Boolean = false) {
        if (view == null) return
        val tvLabel: TextView? = view.findViewById(R.id.tvLabel)
        val tvValue: TextView? = view.findViewById(R.id.tvValue)
        
        tvLabel?.text = label
        tvValue?.text = value
        
        if (isBold) {
            tvValue?.setTextColor(resources.getColor(R.color.brand_red))
            tvValue?.textSize = 16f
        } else {
            tvValue?.setTextColor(resources.getColor(R.color.black))
            tvValue?.textSize = 14f
        }
    }
}
