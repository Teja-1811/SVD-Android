package com.svd.svdagencies.ui.user

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.api.user.UserApi
import com.svd.svdagencies.data.model.user.UserBillDetailResponse
import com.svd.svdagencies.ui.user.adapter.UserBillItemAdapter
import com.svd.svdagencies.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserBillDetailActivity : AppCompatActivity() {

    private lateinit var tvInvoiceNum: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvOpeningDue: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvLastPaid: TextView
    private lateinit var tvCurrentDue: TextView
    private lateinit var rvItems: RecyclerView
    private lateinit var btnDownload: MaterialButton

    private lateinit var sessionManager: SessionManager
    private lateinit var api: UserApi
    private var billId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_bill_details)

        billId = intent.getIntExtra("BILL_ID", -1)
        if (billId == -1) {
            Toast.makeText(this, "Invalid Bill ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        sessionManager = SessionManager(this)
        api = ApiClient.retrofit.create(UserApi::class.java)

        initViews()
        loadBillDetails()
    }

    private fun initViews() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        tvInvoiceNum = findViewById(R.id.tvDetailInvoiceNum)
        tvDate = findViewById(R.id.tvDetailDate)
        tvOpeningDue = findViewById(R.id.tvDetOpeningDue)
        tvTotalAmount = findViewById(R.id.tvDetTotalAmount)
        tvLastPaid = findViewById(R.id.tvDetLastPaid)
        tvCurrentDue = findViewById(R.id.tvDetCurrentDue)
        rvItems = findViewById(R.id.rvBillItems)
        btnDownload = findViewById(R.id.btnDownloadPdf)

        rvItems.layoutManager = LinearLayoutManager(this)
        btnDownload.setOnClickListener { downloadInvoice() }
    }

    private fun loadBillDetails() {
        api.getUserBillDetail(billId).enqueue(object : Callback<UserBillDetailResponse> {
            override fun onResponse(call: Call<UserBillDetailResponse>, response: Response<UserBillDetailResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    displayBill(response.body()!!)
                } else {
                    Toast.makeText(this@UserBillDetailActivity, "Failed to load details", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserBillDetailResponse>, t: Throwable) {
                Toast.makeText(this@UserBillDetailActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayBill(data: UserBillDetailResponse) {
        val bill = data.bill
        tvInvoiceNum.text = "Invoice #${bill.invoiceNumber}"
        tvDate.text = "Date: ${bill.invoiceDate}"
        tvOpeningDue.text = "?%.2f".format(bill.openingDue)
        tvTotalAmount.text = "?%.2f".format(bill.totalAmount)
        tvLastPaid.text = "?%.2f".format(bill.lastPaid)
        tvCurrentDue.text = "?%.2f".format(bill.currentDue)
        rvItems.adapter = UserBillItemAdapter(data.items)
    }

    private fun downloadInvoice() {
        val url = "${ApiClient.retrofit.baseUrl()}api/user/bills/$billId/download/"
        val token = sessionManager.getToken()

        if (token == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("Invoice Detail")
                .setDescription("Downloading...")
                .addRequestHeader("Authorization", "Token $token")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Invoice-$billId.pdf")

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            Toast.makeText(this, "Download started...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
