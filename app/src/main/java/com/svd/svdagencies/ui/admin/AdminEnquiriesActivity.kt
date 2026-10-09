package com.svd.svdagencies.ui.admin

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.AdminEnquiry
import com.svd.svdagencies.ui.admin.adapter.AdminEnquiriesAdapter
import com.svd.svdagencies.utils.AppSwipeRefreshLayout
import com.svd.svdagencies.utils.NetworkMessageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminEnquiriesActivity : AdminBaseActivity() {

    private lateinit var swipeRefresh: AppSwipeRefreshLayout
    private lateinit var tabLayout: TabLayout
    private lateinit var rvEnquiries: RecyclerView
    private lateinit var layoutEmpty: View
    private val adapter = AdminEnquiriesAdapter(::markResolved)
    private var showingResolved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_enquiries)

        setupAdminLayout("Enquiries")
        bindViews()
        setupUi()
        loadEnquiries()
    }

    private fun bindViews() {
        swipeRefresh = findViewById(R.id.swipeRefresh)
        tabLayout = findViewById(R.id.tabLayout)
        rvEnquiries = findViewById(R.id.rvEnquiries)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        rvEnquiries.adapter = adapter
    }

    private fun setupUi() {
        tabLayout.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    showingResolved = tab?.position == 1
                    loadEnquiries()
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
                override fun onTabReselected(tab: TabLayout.Tab?) = Unit
            }
        )

        swipeRefresh.setOnRefreshListener {
            swipeRefresh.isRefreshing = false
            loadEnquiries()
        }
    }

    private fun loadEnquiries() {
        swipeRefresh.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = if (showingResolved) {
                    ApiClient.adminEnquiriesApi.getResolvedEnquiries()
                } else {
                    ApiClient.adminEnquiriesApi.getActiveEnquiries()
                }
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    val items = response.enquiries
                    adapter.submitList(items, resolvedMode = showingResolved)
                    val hasItems = items.isNotEmpty()
                    rvEnquiries.visibility = if (hasItems) View.VISIBLE else View.GONE
                    layoutEmpty.visibility = if (hasItems) View.GONE else View.VISIBLE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    rvEnquiries.visibility = View.GONE
                    layoutEmpty.visibility = View.VISIBLE
                    showToast(NetworkMessageUtils.friendlyMessage(e, "Failed to load enquiries"))
                }
            }
        }
    }

    private fun markResolved(enquiry: AdminEnquiry) {
        showScreenLoading()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.adminEnquiriesApi.updateEnquiryStatus(
                    enquiryId = enquiry.id,
                    body = mapOf("status" to "resolved")
                )
                withContext(Dispatchers.Main) {
                    hideScreenLoading()
                    showToast(response.message)
                    loadEnquiries()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideScreenLoading()
                    showToast(NetworkMessageUtils.friendlyMessage(e, "Failed to update enquiry"))
                }
            }
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
