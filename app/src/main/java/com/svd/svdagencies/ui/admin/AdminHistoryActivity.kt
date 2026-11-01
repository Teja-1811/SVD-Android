package com.svd.svdagencies.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.utils.WhiteMaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class AdminHistoryRecord(val id: Int, val title: String, val subtitle: String,
    val details: List<String> = emptyList(), @SerializedName("can_delete") val canDelete: Boolean = false)
data class AdminHistoryPage(val records: List<AdminHistoryRecord> = emptyList(), val page: Int = 1,
    val count: Int = 0, @SerializedName("has_next") val hasNext: Boolean = false)
interface AdminHistoryApi {
    @GET("api/admin/deleted-customers/") suspend fun deleted(@Query("page") page: Int, @Query("q") query: String): AdminHistoryPage
    @GET("api/admin/orders/history/") suspend fun orders(@Query("page") page: Int, @Query("q") query: String): AdminHistoryPage
    @DELETE("api/admin/orders/history/{id}/") suspend fun delete(@Path("id") id: Int): JsonObject
}

/** Native, paginated history pages for the website's two admin destinations. */
class AdminHistoryActivity : AdminBaseActivity() {
    private val api by lazy { ApiClient.retrofit.create(AdminHistoryApi::class.java) }
    private val deleted get() = intent.getStringExtra("history_kind") == "deleted"
    private val records = mutableListOf<AdminHistoryRecord>()
    private val rows = HistoryAdapter()
    private lateinit var refresh: SwipeRefreshLayout
    private lateinit var status: TextView
    private lateinit var more: MaterialButton
    private lateinit var search: TextInputEditText
    private var page = 0
    private var request: Job? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_history)
        setupAdminLayout(if (deleted) "Deleted Customers" else "Orders History")
        refresh = findViewById(R.id.historyRefresh)
        status = findViewById(R.id.historyStatus)
        more = findViewById(R.id.historyMore)
        search = findViewById(R.id.historySearch)
        findViewById<RecyclerView>(R.id.historyRecords).apply {
            layoutManager = LinearLayoutManager(this@AdminHistoryActivity); adapter = rows
        }
        refresh.setOnRefreshListener { load(true) }
        more.setOnClickListener { load(false) }
        search.doAfterTextChanged { load(true, 300) }
        load(true)
    }
    private fun load(reset: Boolean, debounce: Long = 0) {
        request?.cancel()
        request = lifecycleScope.launch {
            delay(debounce)
            refresh.isRefreshing = true; more.isEnabled = false
            try {
                val result = if (deleted) api.deleted(if (reset) 1 else page + 1, search.text.toString().trim())
                    else api.orders(if (reset) 1 else page + 1, search.text.toString().trim())
                if (reset) records.clear()
                records.addAll(result.records); page = result.page; rows.notifyDataSetChanged()
                status.text = if (records.isEmpty()) "No records match your search." else "${records.size} of ${result.count} records"
                more.visibility = if (result.hasNext) View.VISIBLE else View.GONE
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                status.text = if (error is HttpException && error.code() == 404)
                    "History is unavailable on this server. Update the backend and swipe down to retry."
                    else "Could not load history. Swipe down to retry."
            } finally { refresh.isRefreshing = false; more.isEnabled = true }
        }
    }
    private fun confirmDelete(record: AdminHistoryRecord) {
        WhiteMaterialAlertDialogBuilder(this).setTitle("Delete ${record.title}?")
            .setMessage("Only this payment-pending order will be removed.")
            .setNegativeButton("Cancel", null).setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try { api.delete(record.id); load(true) }
                    catch (error: Exception) {
                        if (error is CancellationException) throw error
                        status.text = "Could not delete the order. It may no longer be payment pending."
                    }
                }
            }.show()
    }
    private inner class HistoryAdapter : RecyclerView.Adapter<HistoryHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, type: Int) = HistoryHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_admin_history, parent, false))
        override fun getItemCount() = records.size
        override fun onBindViewHolder(holder: HistoryHolder, position: Int) {
            val record = records[position]
            holder.title.text = record.title; holder.subtitle.text = record.subtitle
            holder.details.text = record.details.joinToString("\n")
            holder.delete.visibility = if (!deleted && record.canDelete) View.VISIBLE else View.GONE
            holder.delete.setOnClickListener { confirmDelete(record) }
        }
    }
    private class HistoryHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title = view.findViewById<TextView>(R.id.historyRecordTitle)
        val subtitle = view.findViewById<TextView>(R.id.historyRecordSubtitle)
        val details = view.findViewById<TextView>(R.id.historyRecordDetails)
        val delete = view.findViewById<MaterialButton>(R.id.historyRecordDelete)
    }
}
