package com.svd.svdagencies.ui.customer.fragment

import android.app.DatePickerDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.api.customer.InvoiceApi
import com.svd.svdagencies.data.model.customer.InvoiceItem
import com.svd.svdagencies.data.model.customer.InvoiceResponse
import com.svd.svdagencies.ui.customer.adapter.BillsAdapter
import com.svd.svdagencies.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class CustomerBillsFragment : Fragment(R.layout.fragment_customer_bills) {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvBills: RecyclerView
    private lateinit var tvStatus: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var etMonth: EditText
    private lateinit var btnFilter: Button
    private lateinit var tvTotalBills: TextView
    private lateinit var tvTotalAmount: TextView

    private lateinit var adapter: BillsAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var api: InvoiceApi
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

        // ---------- INIT ----------
        swipeRefresh = view.findViewById(R.id.billsSwipeRefresh)
        rvBills = view.findViewById(R.id.rvBills)
        tvStatus = view.findViewById(R.id.tvStatus)
        tvEmpty = view.findViewById(R.id.tvEmptyBills)
        etMonth = view.findViewById(R.id.etMonth)
        btnFilter = view.findViewById(R.id.btnFilter)
        tvTotalBills = view.findViewById(R.id.tvTotalBills)
        tvTotalAmount = view.findViewById(R.id.tvTotalAmount)

        sessionManager = SessionManager(requireContext())
        api = ApiClient.retrofit.create(InvoiceApi::class.java)

        adapter = BillsAdapter(mutableListOf()) { bill, action ->
            when (action) {
                "view" -> viewInvoice(bill)
                "download" -> downloadInvoice(bill)
            }
        }
        rvBills.layoutManager = LinearLayoutManager(requireContext())
        rvBills.adapter = adapter
        rvBills.setHasFixedSize(true)

        // ---------- DEFAULT MONTH ----------
        setCurrentMonth()

        // ---------- ACTIONS ----------
        etMonth.setOnClickListener { showMonthYearPicker() }
        btnFilter.setOnClickListener { loadBills() }

        swipeRefresh.setOnRefreshListener { loadBills() }

        // ---------- INITIAL LOAD ----------
        loadBills()

        ContextCompat.registerReceiver(requireContext(), onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            requireContext().unregisterReceiver(onDownloadComplete)
        } catch (e: Exception) {
            // Receiver not registered
        }
    }

    private fun setCurrentMonth() {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)
        etMonth.setText("$month-$year")   // MM-YYYY
    }

    private fun showMonthYearPicker() {
        val cal = Calendar.getInstance()
        val monthYear = etMonth.text.toString().split("-")
        val year = if (monthYear.size == 2) monthYear[1].toIntOrNull() ?: cal.get(Calendar.YEAR) else cal.get(Calendar.YEAR)
        val month = if (monthYear.size == 2) monthYear[0].toIntOrNull()?.minus(1) ?: cal.get(Calendar.MONTH) else cal.get(Calendar.MONTH)

        val dpd = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, _ ->
                etMonth.setText("${selectedMonth + 1}-$selectedYear")
                loadBills()
            },
            year,
            month,
            1
        )
        dpd.show()
    }

    private fun loadBills() {
        tvStatus.text = "Loading bills..."
        swipeRefresh.isRefreshing = true

        val monthYear = etMonth.text.toString().split("-")
        if (monthYear.size != 2) {
            tvStatus.text = "Invalid month format (MM-YYYY)"
            swipeRefresh.isRefreshing = false
            return
        }

        val month = monthYear[0]
        val year = monthYear[1]

        api.getCustomerInvoices(
            month = month,
            year = year
        ).enqueue(object : Callback<InvoiceResponse> {

            override fun onResponse(
                call: Call<InvoiceResponse>,
                response: Response<InvoiceResponse>
            ) {
                swipeRefresh.isRefreshing = false

                if (response.isSuccessful && response.body() != null) {
                    val invoices = response.body()!!.invoices

                    adapter.updateData(invoices)

                    tvEmpty.visibility =
                        if (invoices.isEmpty()) View.VISIBLE else View.GONE

                    tvStatus.text =
                        if (invoices.isEmpty()) "No bills found"
                        else "Showing ${invoices.size} bills"

                    // Update stats
                    val totalBills = invoices.size
                    val totalAmount = invoices.sumOf { it.amount }
                    tvTotalBills.text = totalBills.toString()
                    tvTotalAmount.text = "₹${String.format("%.2f", totalAmount)}"

                } else {
                    tvStatus.text = "Failed to load bills"
                }
            }

            override fun onFailure(call: Call<InvoiceResponse>, t: Throwable) {
                swipeRefresh.isRefreshing = false
                tvStatus.text = "Network error"
            }
        })
    }

    private fun viewInvoice(bill: InvoiceItem) {
        downloadInvoice(bill, openAfterDownload = true)
    }

    private fun downloadInvoice(bill: InvoiceItem, openAfterDownload: Boolean = false) {
        val url = "${ApiClient.retrofit.baseUrl()}api/customer/invoice/download/?invoice_number=${bill.number}"
        val token = sessionManager.getToken()

        if (token == null) {
            Toast.makeText(requireContext(), "Authentication error", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("Invoice #${bill.number}")
                .setDescription("Downloading invoice...")
                .addRequestHeader("Authorization", "Token $token")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Invoice-${bill.number}.pdf")

            val downloadManager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            if (openAfterDownload) {
                downloadIdsToOpen.add(downloadId)
            }

            Toast.makeText(requireContext(), "Download started...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to start download: ${e.message}", Toast.LENGTH_LONG).show()
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
            Toast.makeText(requireContext(), "No PDF viewer found", Toast.LENGTH_SHORT).show()
        }
    }
}
