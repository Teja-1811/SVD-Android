package com.svd.svdagencies.ui.admin.sales

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.admin.CategorySalesResponse
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CategorySalesActivity : AdminBaseActivity() {
    private lateinit var content: LinearLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var selectedPeriod = "overall"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_simple_dashboard)
        setupAdminLayout("Category Sales")
        val root = findViewById<LinearLayout>(R.id.contentContainer)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        root.addView(TextView(this).apply {
            text = "Category Sales\nReview sales volume and value by product category."
            textSize = 18f
            setPadding(8, 8, 8, 24)
        })
        val periods = listOf("overall", "today", "week", "month", "year")
        root.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@CategorySalesActivity, android.R.layout.simple_spinner_dropdown_item, periods)
            setSelection(0)
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    selectedPeriod = periods[position]
                    load(selectedPeriod)
                }
            }
        })
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(content)
        swipeRefresh.setOnRefreshListener { load(selectedPeriod) }
        load("overall")
    }

    private fun load(period: String) = ApiClient.categorySalesApi.getSummary(period).enqueue(object : Callback<CategorySalesResponse> {
        override fun onResponse(call: Call<CategorySalesResponse>, response: Response<CategorySalesResponse>) {
            swipeRefresh.isRefreshing = false
            if (!response.isSuccessful) return failure()
            val data = response.body() ?: return failure()
            content.removeAllViews()
            content.addView(TextView(this@CategorySalesActivity).apply { text = "Total volume: %.3f L".format(data.total_volume); textSize = 20f })
            data.categories.forEach { category ->
                content.addView(TextView(this@CategorySalesActivity).apply { text = "$category  •  %.3f L  •  ₹%.2f".format(data.data_by_category[category] ?: 0.0, data.amount_by_category[category] ?: 0.0); textSize = 16f; setPadding(0, 16, 0, 16) })
            }
        }
        override fun onFailure(call: Call<CategorySalesResponse>, error: Throwable) { swipeRefresh.isRefreshing = false; failure() }
    })

    private fun failure() = Toast.makeText(this, "Unable to load category sales", Toast.LENGTH_SHORT).show()
}
