package com.svd.svdagencies.ui.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import com.svd.svdagencies.utils.WhiteMaterialAlertDialogBuilder as MaterialAlertDialogBuilder
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.card.MaterialCardView
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
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

class AdminMonthlySummary : AdminBaseActivity() {

    private lateinit var autoRoute: AutoCompleteTextView
    private var selectedRouteId: Int? = null
    private lateinit var autoCustomer: AutoCompleteTextView
    private lateinit var etMonth: ImageButton
    private lateinit var btnDownload: ImageButton
    private lateinit var btnWhatsappShare: ImageButton
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
    private var selectedCustomerArea: String = ""
    private val calendar = Calendar.getInstance()
    private var currentSummary: MonthlySummaryResponse? = null
    private var statementView: View? = null
    private var extraSummaryView: View? = null
    private lateinit var monthLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_customer_monthly_summary)
        setupAdminLayout("Monthly Summary")

        api = ApiClient.retrofit.create(CustomerDashboardApi::class.java)

        // Initialize views with null safety
        autoCustomer = findViewById(R.id.autoCustomer) ?: return
        etMonth = findViewById(R.id.etMonth) ?: return
        btnDownload = findViewById(R.id.btnDownload) ?: return
        btnWhatsappShare = findViewById(R.id.btnWhatsappShare) ?: return

        layoutResults = findViewById(R.id.layoutResults) ?: return
        cardCustomerDetails = findViewById(R.id.cardCustomerDetails) ?: return
        cardFinancialSummary = findViewById(R.id.cardFinancialSummary) ?: return
        cardVolumeSummary = findViewById(R.id.cardVolumeSummary) ?: return

        tvDetailName = findViewById(R.id.tvDetailName) ?: return
        tvDetailShop = findViewById(R.id.tvDetailShop) ?: return
        tvDetailPhone = findViewById(R.id.tvDetailPhone) ?: return

        val filterRow = etMonth.parent as? LinearLayout
        val customerField = autoCustomer.parent?.parent as? View
        if (filterRow != null && customerField?.parent == filterRow) {
            filterRow.orientation = LinearLayout.VERTICAL
            filterRow.removeView(customerField)
            val selectors = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL;tag="svd-fixed-row" }
            val routeBox = com.google.android.material.textfield.TextInputLayout(this).apply {
                hint="Route";boxBackgroundMode=com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
                layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply { marginEnd=(8*resources.displayMetrics.density).toInt() }
            }
            autoRoute = com.google.android.material.textfield.MaterialAutoCompleteTextView(this).apply { inputType=0;hint="Route";threshold=0;keyListener=null;setOnClickListener { showDropDown() };setOnFocusChangeListener { _,focused -> if(focused)showDropDown() } }
            routeBox.addView(autoRoute,LinearLayout.LayoutParams(-1,-2))
            routeBox.endIconMode=com.google.android.material.textfield.TextInputLayout.END_ICON_DROPDOWN_MENU
            selectors.addView(routeBox)
            customerField.layoutParams=LinearLayout.LayoutParams(0,-2,1f)
            selectors.addView(customerField)
            filterRow.addView(selectors)
            val actions = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.END;tag="svd-fixed-row";setPadding(0,(8*resources.displayMetrics.density).toInt(),0,0) }
            listOf(etMonth, btnDownload, btnWhatsappShare).forEach { button ->
                filterRow.removeView(button)
                val size=(32*resources.displayMetrics.density).toInt()
                button.minimumWidth=0;button.minimumHeight=0
                button.setPadding(6,6,6,6)
                actions.addView(button, LinearLayout.LayoutParams(size,size).apply { marginStart=(8*resources.displayMetrics.density).toInt() })
            }
            filterRow.addView(actions)
        }
        monthLabel = TextView(this).apply { textSize=14f; setPadding(12,16,12,16) }
        layoutResults.addView(monthLabel, 0)
        etMonth.setOnClickListener { showMonthPicker() }
        
        loadCustomers()

        btnDownload.setOnClickListener {
            downloadPdf()
        }

        btnWhatsappShare.setOnClickListener {
            if (currentSummary == null) {
                Toast.makeText(this, "Please select a customer and month first", Toast.LENGTH_SHORT).show()
            } else {
                shareOnWhatsapp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        fetchSummary()
    }

    private fun showMonthPicker() {
        val dialogView = layoutInflater.inflate(R.layout.admin_monthly_summary_year_picker, null)
        val monthPicker = dialogView.findViewById<NumberPicker>(R.id.monthPicker)
        val yearPicker = dialogView.findViewById<NumberPicker>(R.id.yearPicker)

        val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        monthPicker.minValue = 0
        monthPicker.maxValue = 11
        monthPicker.displayedValues = monthNames
        monthPicker.value = calendar.get(Calendar.MONTH)

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        yearPicker.minValue = currentYear - 5
        yearPicker.maxValue = currentYear + 5
        yearPicker.value = calendar.get(Calendar.YEAR)

        MaterialAlertDialogBuilder(this)
            .setTitle("Select Month and Year")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                calendar.set(Calendar.YEAR, yearPicker.value)
                calendar.set(Calendar.MONTH, monthPicker.value)
                fetchSummary()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadCustomers() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = api.getCustomers().awaitResponse()
                if (response.isSuccessful) {
                    customersList = response.body()?.customers ?: emptyList()

                    withContext(Dispatchers.Main) {
                        if (isDestroyed || isFinishing) return@withContext
                        val routes=customersList.filter { it.route_id!=null }.distinctBy { it.route_id }
                        val routeLabels=listOf("All routes")+routes.map { it.route_name ?: "Route #${it.route_id}" }
                        autoRoute.setAdapter(ArrayAdapter(this@AdminMonthlySummary,R.layout.delivery_dropdown_item,routeLabels))
                        autoRoute.setText("All routes",false)
                        autoRoute.setOnItemClickListener { _,_,position,_ ->
                            selectedRouteId=if(position==0)null else routes[position-1].route_id
                            selectedCustomerId=null;currentSummary=null;autoCustomer.setText("",false);layoutResults.visibility=View.GONE
                            updateCustomerFilter()
                        }
                        updateCustomerFilter()
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                withContext(Dispatchers.Main) {
                    if (isDestroyed || isFinishing) return@withContext
                    Toast.makeText(this@AdminMonthlySummary, "Failed to load customers", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateCustomerFilter() {
        val filtered=customersList.filter { selectedRouteId==null || it.route_id==selectedRouteId }
        val names=filtered.map { it.name ?: "Unknown" }
        val adapter=ArrayAdapter(this,R.layout.delivery_dropdown_item,names)
        autoCustomer.setAdapter(adapter)
        autoCustomer.setOnItemClickListener { _,_,position,_ ->
            val customer=filtered.filter { it.name.orEmpty().contains(autoCustomer.text.toString(),true) }.getOrNull(position) ?: filtered.getOrNull(position) ?: return@setOnItemClickListener
            selectedCustomerId=customer.id;selectedCustomerArea=customer.area.orEmpty()
            layoutResults.visibility=View.VISIBLE;cardCustomerDetails.visibility=View.VISIBLE
            tvDetailName.text=customer.name;tvDetailShop.text=customer.shop_name;tvDetailPhone.text=customer.phone
            fetchSummary()
        }
    }

    private fun fetchSummary() {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val date = sdf.format(calendar.time)
        val customerId = selectedCustomerId ?: return
        currentSummary = null
        btnWhatsappShare.isEnabled = false
        monthLabel.text = "Loading $date…"
        cardFinancialSummary.visibility = View.GONE
        cardVolumeSummary.visibility = View.GONE
        statementView?.visibility = View.GONE
        extraSummaryView?.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = api.getMonthlySalesSummary(date, customerId)
                withContext(Dispatchers.Main) {
                    if (isDestroyed || isFinishing) return@withContext
                    if (selectedCustomerId == customerId && SimpleDateFormat("yyyy-MM", Locale.US).format(calendar.time) == date) updateUI(response)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                withContext(Dispatchers.Main) {
                    if (isDestroyed || isFinishing) return@withContext
                    monthLabel.text = "Couldn't load summary. Try again."
                    Toast.makeText(this@AdminMonthlySummary, "Unable to load monthly summary", Toast.LENGTH_SHORT).show()
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

        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val date = sdf.format(calendar.time) // yyyy-MM

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("DownloadPdf", "Starting download for date: $date, customer: $customerId, area: $selectedCustomerArea")
                
                val responseBody = api.downloadMonthlySalesPdf(date, customerId, selectedCustomerArea)
                
                val fileName = "Monthly_Summary_${date}_$customerId.pdf"
                val file = File(getExternalFilesDir(null), fileName)
                
                Log.d("DownloadPdf", "Saving to: ${file.absolutePath}")
                
                withContext(Dispatchers.IO) {
                    responseBody.byteStream().use { inputStream ->
                        FileOutputStream(file).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
                
                if (file.exists() && file.length() > 0) {
                    // Final sanity check: if it's a PDF, it should start with %PDF
                    val firstBytes = file.inputStream().use { 
                        val bytes = ByteArray(4)
                        it.read(bytes)
                        bytes
                    }
                    val isPdf = String(firstBytes) == "%PDF"
                    
                    withContext(Dispatchers.Main) {
                        if (isDestroyed || isFinishing) return@withContext
                        if (isPdf) {
                            Toast.makeText(this@AdminMonthlySummary, "PDF Downloaded", Toast.LENGTH_SHORT).show()
                            openPdf(file)
                        } else {
                            // If it's not a PDF, it's likely the HTML login page or an error page
                            file.delete()
                            Toast.makeText(this@AdminMonthlySummary, "Failed to download: Invalid response from server", Toast.LENGTH_LONG).show()
                            Log.e("DownloadPdf", "Downloaded file is not a PDF. Likely HTML redirect.")
                        }
                    }
                } else {
                    throw Exception("File creation failed or empty file")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("DownloadPdf", "Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (isDestroyed || isFinishing) return@withContext
                    Toast.makeText(this@AdminMonthlySummary, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openPdf(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("OpenPdf", "Error opening PDF", e)
            Toast.makeText(this, "No app found to open PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareOnWhatsapp() {
        val summary = currentSummary ?: return
        val phoneNumber = summary.customer.phone ?: return
        
        // Clean phone number (keep only digits)
        val cleanPhone = phoneNumber.replace(Regex("[^0-9]"), "")
        val finalPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone

        val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
        val message = """
            *Monthly Sales Summary - $monthName*
            
            *Customer:* ${summary.customer.name}
            
            *Financials:*
            Opening Due: ₹${String.format("%.2f", summary.summary.opening_due)}
            Total Sales: ₹${String.format("%.2f", summary.summary.total_sales)}
            Paid Amount: ₹${String.format("%.2f", summary.summary.paid_amount)}
            Net Due: ₹${String.format("%.2f", summary.summary.due_amount)}
            *Remaining Balance: ₹${String.format("%.2f", summary.summary.remaining_due)}*
            
            *Volumes:*
            Milk: ${String.format("%.2f", summary.volume.milk_volume)} L
            Curd: ${String.format("%.2f", summary.volume.curd_volume)} L
        """.trimIndent()

        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val url = "https://api.whatsapp.com/send?phone=$finalPhone&text=${URLEncoder.encode(message, "UTF-8")}"
            intent.data = Uri.parse(url)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI(data: MonthlySummaryResponse) {
        currentSummary = data
        btnWhatsappShare.isEnabled = true
        layoutResults.visibility = View.VISIBLE
        cardCustomerDetails.visibility = View.VISIBLE
        cardFinancialSummary.visibility = View.VISIBLE
        cardVolumeSummary.visibility = View.VISIBLE

        // Update details from API response
        tvDetailName.text = data.customer.name
        tvDetailPhone.text = data.customer.phone
        tvDetailShop.text = data.customer.shop_name ?: tvDetailShop.text
        monthLabel.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
        statementView?.let { layoutResults.removeView(it) }
        statementView = MonthlyStatementTable.create(this, data.statement_groups.orEmpty()).also {
            layoutResults.addView(it, layoutResults.indexOfChild(cardVolumeSummary).coerceAtLeast(0))
        }

        extraSummaryView?.let { layoutResults.removeView(it) }
        extraSummaryView = TextView(this).apply {
            text = "Discounts: ₹${String.format("%.2f", data.summary.total_discount)}\nPeriod profit: ₹${String.format("%.2f", data.summary.selected_period_profit)} · Lifetime profit: ₹${String.format("%.2f", data.summary.overall_customer_profit)}"
            textSize = 13f
            setPadding(16,16,16,16)
        }.also { layoutResults.addView(it, layoutResults.indexOfChild(cardFinancialSummary) + 1) }
        // Financial Summary
        setupRow(findViewById(R.id.rowOpeningDue), "Account balance before period", "₹${String.format("%.2f", data.summary.opening_due)}")
        setupRow(findViewById(R.id.rowTotalSales), "Total Sales", "₹${String.format("%.2f", data.summary.total_sales)}")
        setupRow(findViewById(R.id.rowPaidAmount), "Paid Amount", "₹${String.format("%.2f", data.summary.paid_amount)}")
        setupRow(findViewById(R.id.rowDueAmount), "Month balance", "₹${String.format("%.2f", data.summary.due_amount)}")
        setupRow(findViewById(R.id.rowRemainingDue), "Current account balance", "₹${String.format("%.2f", data.summary.remaining_due)}", true)

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
            tvValue?.setTextColor(ContextCompat.getColor(this, R.color.brand_red))
            tvValue?.textSize = 14f
        } else {
            tvValue?.setTextColor(ContextCompat.getColor(this, R.color.black))
            tvValue?.textSize = 13f
        }
    }
}
