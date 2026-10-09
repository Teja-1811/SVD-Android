package com.svd.svdagencies.ui.customer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.customer.RaisedQueriesResponse
import com.svd.svdagencies.data.model.customer.SupportTicketSummaryResponse
import com.svd.svdagencies.ui.auth.LoginActivity
import com.svd.svdagencies.ui.customer.CustomerContactSupportActivity
import com.svd.svdagencies.ui.customer.CustomerStatementActivity
import com.svd.svdagencies.ui.customer.adapter.RaisedQueriesAdapter
import com.svd.svdagencies.utils.SessionManager
import com.svd.svdagencies.base.BaseActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CustomerRaisedQueriesActivity : BaseActivity() {

    private val queriesAdapter = RaisedQueriesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.customer)

        val drawerLayout = findViewById<DrawerLayout>(R.id.customerDrawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.customerNavigationView)
        findViewById<ImageButton>(R.id.btnCustomerMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        navigationView.getHeaderView(0).findViewById<View>(R.id.btnCloseDrawer).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        navigationView.setCheckedItem(R.id.nav_queries)
        val session = SessionManager(this)
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    startActivity(Intent(this, CustomerMainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_terms -> {
                    openDrawerDestination(drawerLayout, TermsConditionsActivity::class.java)
                    true
                }
                R.id.nav_company -> {
                    openDrawerDestination(drawerLayout, CustomerCompanyDetailsActivity::class.java)
                    true
                }
                R.id.nav_support -> {
                    openDrawerDestination(drawerLayout, CustomerContactSupportActivity::class.java)
                    true
                }
                R.id.nav_queries -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_statement -> {
                    openDrawerDestination(drawerLayout, CustomerStatementActivity::class.java)
                    true
                }
                R.id.nav_logout -> {
                    session.logout()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }

        findViewById<ImageButton>(R.id.btnCustomerLogout).setOnClickListener {
            session.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        findViewById<TextView>(R.id.tvCustomerToolbarTitle).text = "Raised Queries"
        findViewById<BottomNavigationView>(R.id.customerBottomNav).visibility = View.GONE

        val container = findViewById<FrameLayout>(R.id.customerFragmentContainer)
        val inflatedView = layoutInflater.inflate(R.layout.activity_raised_queries, container, false)
        container.addView(inflatedView)

        val tvTotal = inflatedView.findViewById<TextView>(R.id.tvTotalQueries)
        val tvRaised = inflatedView.findViewById<TextView>(R.id.tvRaisedQueries)
        val tvResolved = inflatedView.findViewById<TextView>(R.id.tvResolvedQueries)
        val tvNoQueries = inflatedView.findViewById<TextView>(R.id.tvNoQueries)
        val rvRaisedQueries = inflatedView.findViewById<RecyclerView>(R.id.rvRaisedQueries)
        val btnRaiseQuery = inflatedView.findViewById<MaterialButton>(R.id.btnRaiseQuery)

        rvRaisedQueries.layoutManager = LinearLayoutManager(this)
        rvRaisedQueries.adapter = queriesAdapter

        btnRaiseQuery.setOnClickListener {
            startActivity(Intent(this, CustomerContactSupportActivity::class.java))
        }

        fetchTicketSummary(tvTotal, tvRaised, tvResolved)
        fetchRaisedQueries(tvNoQueries, rvRaisedQueries)
    }

    private fun fetchTicketSummary(tvTotal: TextView, tvRaised: TextView, tvResolved: TextView) {
        ApiClient.customerApi.getSupportTicketSummary()
            .enqueue(object : Callback<SupportTicketSummaryResponse> {
                override fun onResponse(
                    call: Call<SupportTicketSummaryResponse>,
                    response: Response<SupportTicketSummaryResponse>
                ) {
                    if (!response.isSuccessful) return
                    val data = response.body() ?: return
                    tvTotal.text = data.totalTickets.toString()
                    tvRaised.text = data.raisedTickets.toString()
                    tvResolved.text = data.resolvedTickets.toString()
                }

                override fun onFailure(call: Call<SupportTicketSummaryResponse>, t: Throwable) = Unit
            })
    }

    private fun fetchRaisedQueries(tvNoQueries: TextView, rvRaisedQueries: RecyclerView) {
        ApiClient.customerApi.getRaisedQueries().enqueue(object : Callback<RaisedQueriesResponse> {
            override fun onResponse(
                call: Call<RaisedQueriesResponse>,
                response: Response<RaisedQueriesResponse>
            ) {
                if (!response.isSuccessful) return
                val payload = response.body() ?: return
                val queries = payload.queries
                queriesAdapter.submitList(queries)
                val hasQueries = queries.isNotEmpty()
                tvNoQueries.visibility = if (hasQueries) View.GONE else View.VISIBLE
                rvRaisedQueries.visibility = if (hasQueries) View.VISIBLE else View.GONE
            }

            override fun onFailure(call: Call<RaisedQueriesResponse>, t: Throwable) = Unit
        })
    }

    private fun openDrawerDestination(
        drawerLayout: DrawerLayout,
        activityClass: Class<out AppCompatActivity>
    ) {
        drawerLayout.closeDrawer(GravityCompat.START)
        drawerLayout.post {
            startActivity(Intent(this, activityClass))
        }
    }
}
