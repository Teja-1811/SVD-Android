package com.svd.svdagencies.ui.customer

import com.svd.svdagencies.utils.SharedDatePickerDialog as DatePickerDialog
import android.content.Intent
import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.customer.CustomerStatementResponse
import com.svd.svdagencies.ui.auth.LoginActivity
import com.svd.svdagencies.ui.customer.adapter.MonthlyStatementAdapter
import com.svd.svdagencies.utils.RefreshManager
import com.svd.svdagencies.utils.SessionManager
import com.svd.svdagencies.base.BaseActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CustomerStatementActivity : BaseActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var swipeRefresh: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    
    private lateinit var tvSelectedMonth: MaterialButton
    private lateinit var tvOpeningDue: TextView
    private lateinit var tvClosingDue: TextView
    private lateinit var tvTotalBilled: TextView
    private lateinit var tvTotalPaid: TextView
    
    private val statementAdapter = MonthlyStatementAdapter()
    
    private val selectedDate = Calendar.getInstance()
    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.customer_statement)

        initViews()
        setupDrawer()
        setupRecyclerViews()
        setupListeners()

        fetchStatement()
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.customerDrawerLayout)
        navigationView = findViewById(R.id.customerNavigationView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        
        tvSelectedMonth = findViewById(R.id.tvSelectedMonth)
        tvOpeningDue = findViewById(R.id.tvOpeningDue)
        tvClosingDue = findViewById(R.id.tvClosingDue)
        tvTotalBilled = findViewById(R.id.tvTotalBilled)
        tvTotalPaid = findViewById(R.id.tvTotalPaid)

        findViewById<TextView>(R.id.tvCustomerToolbarTitle).text = "Account Statement"
    }

    private fun setupDrawer() {
        findViewById<android.widget.ImageButton>(R.id.btnCustomerMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        
        navigationView.getHeaderView(0).findViewById<View>(R.id.btnCloseDrawer).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        navigationView.setCheckedItem(R.id.nav_statement)
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(PortalNavigation.homeIntent(this))
                    finishAffinity()
                    true
                }
                R.id.nav_terms -> {
                    openDrawerDestination(TermsConditionsActivity::class.java)
                    true
                }
                R.id.nav_company -> {
                    openDrawerDestination(CustomerCompanyDetailsActivity::class.java)
                    true
                }
                R.id.nav_support -> {
                    openDrawerDestination(CustomerContactSupportActivity::class.java)
                    true
                }
                R.id.nav_queries -> {
                    openDrawerDestination(CustomerRaisedQueriesActivity::class.java)
                    true
                }
                R.id.nav_statement -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_logout -> {
                    handleLogout()
                    true
                }
                else -> false
            }
        }
    }

    private fun handleLogout() {
        SessionManager(this).logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupRecyclerViews() {
        findViewById<RecyclerView>(R.id.rvInvoices).apply {
            layoutManager = LinearLayoutManager(this@CustomerStatementActivity)
            adapter = statementAdapter
        }
        findViewById<RecyclerView>(R.id.rvPayments).visibility = View.GONE
        findViewById<View>(R.id.tvPaymentsHeading).visibility = View.GONE
    }

    private fun setupListeners() {
        findViewById<MaterialButton>(R.id.btnPrevMonth).setOnClickListener {
            selectedDate.add(Calendar.MONTH, -1)
            fetchStatement()
        }
        findViewById<MaterialButton>(R.id.btnNextMonth).setOnClickListener {
            selectedDate.add(Calendar.MONTH, 1)
            fetchStatement()
        }
        tvSelectedMonth.setOnClickListener {
            showMonthPicker()
        }
        findViewById<MaterialButton>(R.id.btnDownloadStatement).setOnClickListener {
            downloadStatement()
        }
        
        RefreshManager.setupRefresh(swipeRefresh) {
            fetchStatement()
        }

        findViewById<android.widget.ImageButton>(R.id.btnCustomerLogout).setOnClickListener {
            handleLogout()
        }
    }

    private fun showMonthPicker() {
        DatePickerDialog(
            this,
            { _, year, month, _ ->
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, month)
                fetchStatement()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun fetchStatement() {
        tvSelectedMonth.text = monthFormat.format(selectedDate.time)
        RefreshManager.startRefresh(swipeRefresh)

        val month = selectedDate.get(Calendar.MONTH) + 1
        val year = selectedDate.get(Calendar.YEAR)

        ApiClient.customerApi.getStatement(month, year).enqueue(object : Callback<CustomerStatementResponse> {
            override fun onResponse(
                call: Call<CustomerStatementResponse>,
                response: Response<CustomerStatementResponse>
            ) {
                RefreshManager.stopRefresh(swipeRefresh)
                if (response.isSuccessful) {
                    response.body()?.let { bindData(it) }
                } else {
                    Toast.makeText(this@CustomerStatementActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CustomerStatementResponse>, t: Throwable) {
                RefreshManager.stopRefresh(swipeRefresh)
                Toast.makeText(this@CustomerStatementActivity, "Network Error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun downloadStatement() {
        val token = SessionManager(this).getToken()
        if (token.isNullOrBlank()) {
            Toast.makeText(this, "Your session has expired", Toast.LENGTH_SHORT).show()
            return
        }
        val month = selectedDate.get(Calendar.MONTH) + 1
        val year = selectedDate.get(Calendar.YEAR)
        val url = "${ApiClient.retrofit.baseUrl()}api/customer/statement/pdf/?month=$month&year=$year"
        val request = DownloadManager.Request(Uri.parse(url))
            .addRequestHeader("Authorization", "Token $token")
            .setTitle("Monthly statement")
            .setDescription(monthFormat.format(selectedDate.time))
            .setMimeType("application/pdf")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "statement_${year}_${month}.pdf")
        (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        Toast.makeText(this, "Statement download started", Toast.LENGTH_SHORT).show()
    }

    private fun bindData(data: CustomerStatementResponse) {
        val summary = data.monthlyStatement
        tvOpeningDue.text = "₹%.2f".format(summary?.openingDue ?: data.summary.openingDue)
        tvClosingDue.text = "₹%.2f".format(summary?.remainingDue ?: data.summary.closingDue)
        tvTotalBilled.text = "₹%.2f".format(summary?.totalSales ?: data.summary.totalBilled)
        tvTotalPaid.text = "₹%.2f".format(summary?.paidAmount ?: data.summary.totalPaid)
        statementAdapter.submitList(data.statementGroups.orEmpty())
    }

    private fun openDrawerDestination(activityClass: Class<out AppCompatActivity>) {
        drawerLayout.closeDrawer(GravityCompat.START)
        if (this::class.java != activityClass) {
            startActivity(Intent(this, activityClass))
            finish()
        }
    }
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        PortalNavigation.configure(this, R.id.nav_statement)
    }

}
