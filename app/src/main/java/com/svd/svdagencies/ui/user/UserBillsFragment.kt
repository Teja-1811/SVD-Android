package com.svd.svdagencies.ui.user

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.api.user.UserApi
import com.svd.svdagencies.data.model.user.UserBill
import com.svd.svdagencies.data.model.user.UserBillsResponse
import com.svd.svdagencies.ui.user.adapter.UserBillsAdapter
import com.svd.svdagencies.utils.RefreshManager
import com.svd.svdagencies.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserBillsFragment : Fragment(R.layout.user_bills) {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvBills: RecyclerView
    private lateinit var tvStatus: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var tvTotalBills: TextView
    private lateinit var tvTotalAmount: TextView

    private lateinit var adapter: UserBillsAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var api: UserApi
    private val downloadIdsToOpen = mutableSetOf<Long>()

    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadIdsToOpen.contains(id)) {
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val uri = downloadManager.getUriForDownloadedFile(id)
                if (uri != null) {
                    openFile(uri)
                }
                downloadIdsToOpen.remove(id)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh = view.findViewById(R.id.billsSwipeRefresh)
        rvBills = view.findViewById(R.id.rvBills)
        tvStatus = view.findViewById(R.id.tvStatus)
        tvEmpty = view.findViewById(R.id.tvEmptyBills)
        tvTotalBills = view.findViewById(R.id.tvTotalBills)
        tvTotalAmount = view.findViewById(R.id.tvTotalAmount)

        sessionManager = SessionManager(requireContext())
        api = ApiClient.retrofit.create(UserApi::class.java)

        adapter = UserBillsAdapter(mutableListOf()) { bill, action ->
            when (action) {
                "view" -> openBillDetails(bill)
                "download" -> downloadInvoice(bill)
            }
        }
        rvBills.layoutManager = LinearLayoutManager(requireContext())
        rvBills.adapter = adapter

        RefreshManager.setupRefresh(swipeRefresh) {
            loadBills()
        }

        loadBills()

        ContextCompat.registerReceiver(
            requireContext(),
            onDownloadComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            requireContext().unregisterReceiver(onDownloadComplete)
        } catch (e: Exception) {
            // Receiver not registered
        }
    }

    private fun loadBills() {
        tvStatus.text = "Syncing..."
        RefreshManager.startRefresh(swipeRefresh)

        api.getUserBills().enqueue(object : Callback<UserBillsResponse> {
            override fun onResponse(call: Call<UserBillsResponse>, response: Response<UserBillsResponse>) {
                if (!isAdded) return
                RefreshManager.stopRefresh(swipeRefresh)

                if (response.isSuccessful && response.body() != null) {
                    val bills = response.body()!!.bills
                    adapter.updateData(bills)
                    tvEmpty.visibility = if (bills.isEmpty()) View.VISIBLE else View.GONE
                    tvTotalBills.text = bills.size.toString()
                    val currentDue = if (bills.isNotEmpty()) bills.first().currentDue else 0.0
                    tvTotalAmount.text = "?%.2f".format(currentDue)
                    tvStatus.text = "Updated just now"
                } else {
                    tvStatus.text = "Failed to load"
                }
            }

            override fun onFailure(call: Call<UserBillsResponse>, t: Throwable) {
                if (!isAdded) return
                RefreshManager.stopRefresh(swipeRefresh)
                tvStatus.text = "Network error"
            }
        })
    }

    private fun openBillDetails(bill: UserBill) {
        val intent = Intent(requireContext(), UserBillDetailActivity::class.java)
        intent.putExtra("BILL_ID", bill.id)
        startActivity(intent)
    }

    private fun downloadInvoice(bill: UserBill) {
        val context = context ?: return
        val url = "${ApiClient.retrofit.baseUrl()}api/user/bills/${bill.id}/download/"
        val token = sessionManager.getToken()

        if (token == null) {
            Toast.makeText(context, "Session expired", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("Invoice #${bill.invoiceNumber}")
                .setDescription("Downloading invoice...")
                .addRequestHeader("Authorization", "Token $token")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Invoice-${bill.invoiceNumber}.pdf")

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openFile(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No PDF viewer found", Toast.LENGTH_SHORT).show()
        }
    }
}
